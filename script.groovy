// ============================================================
// AUTO-TRIAGE LISTENER: Client Name + Priority + Ticket Category
// Event: Issue Created (jira:issue_created)
// ============================================================

import com.adaptavist.hapi.cloud.jira.issues.Issue

// ── CONFIG ──────────────────────────────────────────────────
// Replace these values with your own before saving the listener

final String TARGET_PROJECT           = 'DEMO'    // ← Your project key (e.g. 'SUPPORT')
final Long   CLIENT_NAME_FIELD_ID     = 10040L    // ← Your Client Name custom field ID
final Long   TICKET_CATEGORY_FIELD_ID = 10041L    // ← Your Ticket Category custom field ID

// ── CLIENT NAME keywords ─────────────────────────────────────
// Left  = keyword to search for (lowercase, partial match)
// Right = EXACT value of the dropdown option in your Client Name field
final Map<String, String> CLIENT_KEYWORDS = [
    'acme'         : 'Acme Corp',
    'wayne'        : 'Wayne Enterprises',
    // Add more clients here...
] as Map<String, String>

// ── PRIORITY keywords ────────────────────────────────────────
// Left  = keyword to search for (case-insensitive)
// Right = EXACT priority name valid in your project's priority scheme
// First match wins — put most important keywords at the top
final Map<String, String> PRIORITY_KEYWORDS = [

    // ── HIGHEST ───────────────────────────────────────────
    'outage'            : 'Highest',
    'down'              : 'Highest',
    'not working'       : 'Highest',
    'cannot access'     : 'Highest',
    'server down'       : 'Highest',
    'system down'       : 'Highest',
    'urgent'            : 'Highest',
    'emergency'         : 'Highest',
    'critical'          : 'Highest',

    // ── HIGH ──────────────────────────────────────────────
    'broken'            : 'High',
    'error'             : 'High',
    'failed'            : 'High',
    'cannot login'      : 'High',
    'locked out'        : 'High',
    'crashed'           : 'High',

    // ── MEDIUM ────────────────────────────────────────────
    'slow'              : 'Medium',
    'intermittent'      : 'Medium',
    'password'          : 'Medium',
    'printer'           : 'Medium',
    'email'             : 'Medium',
    'vpn'               : 'Medium',
    'network'           : 'Medium',
    'update'            : 'Medium',
    'install'           : 'Medium',

    // ── LOW ───────────────────────────────────────────────
    'question'          : 'Low',
    'how to'            : 'Low',
    'inquiry'           : 'Low',
    'suggestion'        : 'Low',
    'feedback'          : 'Low',
    'minor'             : 'Low',
    'request'           : 'Low',

    // ── LOWEST ────────────────────────────────────────────
    'lowest'            : 'Lowest',

] as Map<String, String>

// ── TICKET CATEGORY keywords ─────────────────────────────────
// Left  = keyword to search for (lowercase, partial match)
// Right = EXACT value of the dropdown option in your Ticket Category field
final Map<String, String> CATEGORY_KEYWORDS = [
    'backup'       : 'Backup',
    'restore'      : 'Backup',
    'network'      : 'Network',
    'connectivity' : 'Network',
    'dns'          : 'Network',
    'password'     : 'Access',
    'access'       : 'Access',
    'login'        : 'Access',
    'patch'        : 'Maintenance',
    'update'       : 'Maintenance',
    'upgrade'      : 'Maintenance',
    // Add more categories here...
] as Map<String, String>

// Default values used when no keyword matches
final String DEFAULT_PRIORITY = 'Medium'   // ← Must be a valid priority in your project
final String DEFAULT_CATEGORY = 'General'  // ← Must be a valid option in your Ticket Category field

// ── END CONFIG ───────────────────────────────────────────────
// Do not edit below this line unless you know what you are doing

// ── STEP 1: Project filter ───────────────────────────────────
def fields     = issue['fields'] as Map
def projectKey = (fields['project'] as Map)['key'] as String
def projectId  = (fields['project'] as Map)['id'] as String

if (projectKey != TARGET_PROJECT) {
    logger.info("Skipping ${issue['key']} — not project ${TARGET_PROJECT}")
    return
}

logger.info("Auto-triage listener triggered for issue: ${issue['key']}")

// ── STEP 2: Load issue and build search text ─────────────────
Issue hapiIssue = Issues.getByKey(issue['key'] as String)
String description = hapiIssue.getDescription() ?: ''
String summary     = hapiIssue.getSummary() ?: ''
String searchText  = (description + ' ' + summary).toLowerCase()

logger.info("Analysing text (first 200 chars): ${searchText.take(200)}")

// ── STEP 3: Match Client Name ────────────────────────────────
String matchedClient = 'Unknown'
for (Map.Entry<String, String> entry : CLIENT_KEYWORDS.entrySet()) {
    if (searchText.contains(entry.key.toLowerCase())) {
        matchedClient = entry.value
        logger.info("Client match: '${entry.key}' → '${matchedClient}'")
        break
    }
}

// ── STEP 4: Match Priority ───────────────────────────────────
String matchedPriorityName = DEFAULT_PRIORITY
for (Map.Entry<String, String> entry : PRIORITY_KEYWORDS.entrySet()) {
    if (searchText.contains(entry.key)) {
        matchedPriorityName = entry.value
        logger.info("Priority match: '${entry.key}' → '${matchedPriorityName}'")
        break
    }
}

// ── STEP 5: Match Ticket Category ───────────────────────────
String matchedCategory = DEFAULT_CATEGORY
for (Map.Entry<String, String> entry : CATEGORY_KEYWORDS.entrySet()) {
    if (searchText.contains(entry.key.toLowerCase())) {
        matchedCategory = entry.value
        logger.info("Category match: '${entry.key}' → '${matchedCategory}'")
        break
    }
}

// ── STEP 6: Resolve priority ID from project's scheme ────────
def schemeResponse = get('/rest/api/3/priorityscheme')
    .queryString('expand', 'priorities,projects')
    .queryString('maxResults', '50')
    .header('Content-Type', 'application/json')
    .asObject(Map)

if (schemeResponse.status != 200) {
    logger.error("Could not fetch priority schemes — status: ${schemeResponse.status}")
    return
}

List schemes = (schemeResponse.body as Map)['values'] as List
Map<String, String> priorityIdByName = [:]

for (Object schemeObj : schemes) {
    Map scheme      = schemeObj as Map
    List projects   = ((scheme['projects'] as Map)['values']) as List
    boolean matches = projects.any { proj -> (proj as Map)['id'] == projectId }
    boolean isDefault = scheme['default'] as boolean

    if (matches || (priorityIdByName.isEmpty() && isDefault)) {
        List priorities = ((scheme['priorities'] as Map)['values']) as List
        for (Object priorityObj : priorities) {
            Map priority = priorityObj as Map
            priorityIdByName[(priority['name'] as String).toLowerCase()] = priority['id'] as String
        }
        if (matches) {
            logger.info("Using priority scheme '${scheme['name']}'")
            break
        }
    }
}

String priorityId = priorityIdByName[matchedPriorityName.toLowerCase()]
if (!priorityId) {
    logger.warn("'${matchedPriorityName}' not in scheme — falling back to '${DEFAULT_PRIORITY}'")
    priorityId = priorityIdByName[DEFAULT_PRIORITY.toLowerCase()]
}

if (!priorityId) {
    logger.error("No valid priority found. Valid options: ${priorityIdByName.keySet()}")
    return
}

// ── STEP 7: Update all three fields in one API call ──────────
String clientFieldKey   = "customfield_${CLIENT_NAME_FIELD_ID}"
String categoryFieldKey = "customfield_${TICKET_CATEGORY_FIELD_ID}"

Map<String, Object> fieldsToUpdate = [
    (clientFieldKey)   : [value: matchedClient],
    (categoryFieldKey) : [value: matchedCategory],
    priority           : [id: priorityId],
] as Map<String, Object>

def response = put("/rest/api/3/issue/${hapiIssue.key}")
    .header('Content-Type', 'application/json')
    .body([fields: fieldsToUpdate])
    .asObject(Map)

if (response.status == 204) {
    logger.info(
        "✅ Done for ${issue['key']}: " +
        "client='${matchedClient}' | " +
        "priority='${matchedPriorityName}' | " +
        "category='${matchedCategory}'"
    )
} else {
    logger.error(
        "Failed to update ${issue['key']} — " +
        "status: ${response.status}, body: ${response.body}"
    )
}
