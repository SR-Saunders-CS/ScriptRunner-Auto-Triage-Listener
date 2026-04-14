# ScriptRunner-Auto-Triage-Listener
Automatically set Priority, Client Name, and Ticket Category on new Jira issues — based on keywords in the issue title and description


# 🤖 ScriptRunner Auto-Triage Listener

> **Automatically set Priority, Client Name, and Ticket Category on new Jira issues — based on keywords in the issue title and description.**

---

## 📋 Table of Contents

1. [What This Does (The Problem It Solves)](#1-what-this-does)
2. [What You Need Before You Start](#2-prerequisites)
3. [Step 1 — Find Your Project Key](#3-step-1--find-your-project-key)
4. [Step 2 — Find Your Custom Field IDs](#4-step-2--find-your-custom-field-ids)
5. [Step 3 — Find Your Valid Priority Names](#5-step-3--find-your-valid-priority-names)
6. [Step 4 — Configure the Script](#6-step-4--configure-the-script)
7. [Step 5 — Create the Listener in ScriptRunner](#7-step-5--create-the-listener-in-scriptrunner)
8. [Step 6 — Test It](#8-step-6--test-it)
9. [Troubleshooting](#9-troubleshooting)
10. [How the Keyword Matching Works](#10-how-the-keyword-matching-works)
11. [Full Script](#11-full-script)

---

## 1. What This Does

### The Problem

When a support ticket arrives in Jira, someone has to manually:
- Read the ticket
- Set the **Priority** (e.g. Critical, High, Medium, Low)
- Set which **Client** the ticket is about
- Set the **Ticket Category** (e.g. Network, Backup, Access)

This is slow, inconsistent, and easy to forget — especially under high volume.

### The Solution

This script runs **automatically the moment a new issue is created** in your chosen Jira project. It reads the issue's **title (summary)** and **description**, scans them for keywords you define, and then sets the three fields for you — instantly, with no human input.

**Example:**
> A ticket arrives with the summary: *"Server down — cannot access file share"*
>
> The script detects:
> - `"server down"` → sets Priority to **Highest**
> - `"cannot access"` → confirms Priority **Highest**
> - `"access"` → sets Ticket Category to **Access**
> - No client keyword found → sets Client Name to **Unknown**

All three fields are updated in a **single API call** before anyone even opens the ticket.

---

## 2. Prerequisites

Before you begin, make sure you have all of the following:

| Requirement | Why You Need It |
|---|---|
| **Jira Cloud** instance | This script only works on Jira Cloud |
| **ScriptRunner for Jira Cloud** installed | This is the plugin that runs the script. Install it from the [Atlassian Marketplace](https://marketplace.atlassian.com/apps/6820/scriptrunner-for-jira) |
| **Jira Administrator** access | You need admin rights to install ScriptRunner and create listeners |
| A **Jira project** to target | The project where new tickets will be auto-triaged |
| A **"Client Name" custom field** (dropdown/select) | Must already exist in Jira. See Step 2 to find its ID |
| A **"Ticket Category" custom field** (dropdown/select) | Must already exist in Jira. See Step 2 to find its ID |

> ⚠️ **Both custom fields must be of type "Select List (single choice)"** and must already be added to the screens used by your target project. If they don't exist yet, create them first via **Jira Settings → Issues → Custom Fields → Create custom field**.

---

## 3. Step 1 — Find Your Project Key

Your project key is the short code that appears before every issue number.

For example, if your issues are named `SUPPORT-123`, your project key is `SUPPORT`.

**To find it:**
1. Go to your Jira project
2. Look at the URL — it will contain something like `/projects/SUPPORT/`
3. Or go to **Project Settings → Details** — the key is shown there

📝 **Write it down:** `PROJECT KEY = ___________`

---

## 4. Step 2 — Find Your Custom Field IDs

You need the **numeric ID** of each custom field. Here's how to find them.

### Method A — Via Jira Settings (Recommended)

1. Go to **Jira Settings** (the cog icon ⚙️ in the top-right)
2. Click **Issues**
3. Click **Custom Fields** (in the left sidebar under "Fields")
4. Find your field (e.g. "Client Name") in the list
5. Click the **three-dot menu (⋯)** next to it → click **Edit**
6. Look at the **URL** in your browser's address bar. It will look like:

   ```
   https://yoursite.atlassian.net/secure/admin/EditCustomField!default.jspa?id=10765
   ```

7. The number at the end (`10765`) is your field ID

📝 **Write them down:**
- `CLIENT NAME FIELD ID = ___________`
- `TICKET CATEGORY FIELD ID = ___________`

### Method B — Via the Jira REST API

If you're comfortable with URLs, paste this into your browser (while logged into Jira):

```
https://YOUR-SITE.atlassian.net/rest/api/3/field
```

This returns a JSON list of all fields. Search (Ctrl+F) for your field name and find the `id` value — it will look like `customfield_10765`. The number after the underscore is your field ID.

---

## 5. Step 3 — Find Your Valid Priority Names

> ⚠️ **This step is critical.** Jira projects use **Priority Schemes** — not every priority name is valid in every project. If you use a priority name that isn't in your project's scheme, the update will fail silently.

### How to Check Your Project's Priorities

1. Go to your Jira project
2. Create a **test issue** manually
3. Look at the **Priority** field dropdown — the options shown are the **only valid priorities** for this project
4. Write down the **exact names** as they appear (spelling and capitalisation matter)

**Common examples:**
- `Highest`, `High`, `Medium`, `Low`, `Lowest`
- `Critical`, `Major`, `Minor`, `Trivial`
- `P1`, `P2`, `P3`, `P4`

📝 **Write down your valid priorities:** `___________`

> 💡 **Note:** The script automatically looks up the correct priority ID from your project's scheme at runtime — you only need to use the correct **names** in the config.

---

## 6. Step 4 — Configure the Script

Now you'll customise the script for your environment. Open the [Full Script](#11-full-script) at the bottom of this guide and edit the **CONFIG section** only. Do not change anything outside the config section unless you know what you're doing.

### 4a — Set Your Project Key

Find this line:
```groovy
final String TARGET_PROJECT = 'DEMO'
```
Replace `DEMO` with your project key from Step 1:
```groovy
final String TARGET_PROJECT = 'SUPPORT'
```

---

### 4b — Set Your Custom Field IDs

Find these lines:
```groovy
final Long CLIENT_NAME_FIELD_ID     = 10040L
final Long TICKET_CATEGORY_FIELD_ID = 10041L
```
Replace the numbers with your field IDs from Step 2. **Keep the `L` at the end** — it tells Groovy these are Long numbers:
```groovy
final Long CLIENT_NAME_FIELD_ID     = 10765L   // ← your Client Name field ID
final Long TICKET_CATEGORY_FIELD_ID = 10802L   // ← your Ticket Category field ID
```

---

### 4c — Configure Client Name Keywords

Find the `CLIENT_KEYWORDS` map. The **left side** is the keyword to search for (lowercase), and the **right side** is the exact dropdown option value in your "Client Name" field:

```groovy
final Map<String, String> CLIENT_KEYWORDS = [
    'scriptrunner' : 'ScriptRunner',
    'connectwise'  : 'ConnectWise',
    'obit'         : 'Obit',
    'backup'       : 'BackupCo',
] as Map<String, String>
```

**How to customise:**
- Replace the example entries with your own clients
- The **right side value must exactly match** the option in your Jira dropdown (including capitalisation)
- The left side keyword is case-insensitive (the script lowercases everything before searching)

**Example — if your clients are "Acme Corp" and "Wayne Enterprises":**
```groovy
final Map<String, String> CLIENT_KEYWORDS = [
    'acme'     : 'Acme Corp',
    'wayne'    : 'Wayne Enterprises',
    'batman'   : 'Wayne Enterprises',
] as Map<String, String>
```

> ⚠️ **The right-side values must exactly match your dropdown options.** Go to your custom field settings and copy the option names exactly.

---

### 4d — Configure Priority Keywords

Find the `PRIORITY_KEYWORDS` map. This works the same way — keyword on the left, priority name on the right:

```groovy
final Map<String, String> PRIORITY_KEYWORDS = [
    'outage'   : 'Highest',
    'down'     : 'Highest',
    'broken'   : 'High',
    'slow'     : 'Medium',
    'question' : 'Low',
] as Map<String, String>
```

**Rules:**
- Right-side values must **exactly match** the priority names in your project (from Step 3)
- Add as many keywords as you like
- Keywords are matched in **order** — the first match wins, so put your most important keywords first
- The script falls back to `DEFAULT_PRIORITY` if no keyword matches

**Set your default priority** (used when no keyword matches):
```groovy
final String DEFAULT_PRIORITY = 'Medium'
```

---

### 4e — Configure Ticket Category Keywords

Find the `CATEGORY_KEYWORDS` map and customise it the same way:

```groovy
final Map<String, String> CATEGORY_KEYWORDS = [
    'backup'       : 'Backup',
    'network'      : 'Network',
    'password'     : 'Access',
    'login'        : 'Access',
] as Map<String, String>
```

- Right-side values must **exactly match** your "Ticket Category" dropdown options
- Set your default category:

```groovy
final String DEFAULT_CATEGORY = 'General'
```

---

## 7. Step 5 — Create the Listener in ScriptRunner

1. In Jira, click the **Apps** menu in the top navigation bar
2. Click **ScriptRunner** → **Listeners**
3. Click **+ Create Listener** (or **Add Listener**)
4. Fill in the form:

   | Field | Value |
   |---|---|
   | **Name** | `Auto-Triage: Priority + Client + Category` (or any name you like) |
   | **Event** | Select **`Issue Created`** (also shown as `jira:issue_created`) |
   | **Condition** | Leave blank |
   | **Script** | Paste your fully configured script here |

5. Click **Save**

> ✅ The listener is now **active immediately** — no restart required.

---

## 8. Step 6 — Test It

### Create a Test Issue

1. Go to your target project
2. Create a new issue
3. In the **Summary**, type something that should trigger a keyword match, for example:
   - `"Server down — urgent help needed"` (should set Priority = Highest)
   - `"Password reset request"` (should set Priority = Medium, Category = Access)

### Check the Result

1. Open the issue you just created
2. Look at the **Priority**, **Client Name**, and **Ticket Category** fields
3. They should be automatically populated within a few seconds

### Check the Logs (If Something Doesn't Work)

1. Go to **Apps → ScriptRunner → Logs** (or **Script Console → Logs**)
2. Look for log entries starting with your issue key (e.g. `SUPPORT-42`)
3. The script logs every match it finds, for example:
   ```
   Combined listener triggered for issue: SUPPORT-42
   Priority match: 'down' → 'Highest'
   Category match: 'access' → 'Access'
   ✅ Done for SUPPORT-42: client='Unknown' | priority='Highest' | category='Access'
   ```

---

## 9. Troubleshooting

### ❌ Fields are not being updated at all

- **Check the project key** — make sure `TARGET_PROJECT` exactly matches your project key (case-sensitive)
- **Check the listener is enabled** — go to ScriptRunner → Listeners and confirm it shows as active
- **Check the logs** — look for error messages in ScriptRunner logs

### ❌ Priority is not updating / getting a 400 error

- The priority name you used in `PRIORITY_KEYWORDS` is **not valid for this project**
- Go back to Step 3 and check the exact priority names available in your project
- Make sure the right-side values in `PRIORITY_KEYWORDS` exactly match (including capitalisation)

### ❌ Client Name or Category is not updating

- The dropdown option value on the right side of your keyword map doesn't match the actual option in Jira
- Go to **Jira Settings → Issues → Custom Fields**, find your field, and check the exact option names
- Copy and paste the option names — don't retype them

### ❌ "Script compilation error" when saving

- Make sure you kept the `L` suffix on field IDs (e.g. `10765L` not `10765`)
- Make sure all map entries have a comma after them except the last one
- Make sure you haven't accidentally deleted a closing bracket `]` or parenthesis `)`

### ❌ The wrong keyword is matching

- Keywords are matched **in order** — the first match wins
- If `'backup'` appears in both `CLIENT_KEYWORDS` and `CATEGORY_KEYWORDS`, both will match independently (they are separate maps)
- Move higher-priority keywords to the **top** of the map

### ❌ Nothing happens and there are no log entries

- The listener may not be saved correctly — go to ScriptRunner → Listeners and re-open it to verify the script is there
- Make sure the event is set to **Issue Created** (not Issue Updated)

---

## 10. How the Keyword Matching Works

Understanding this will help you configure the keywords effectively.

1. When a new issue is created, the script reads the **Summary** and **Description** and joins them into one block of text
2. The entire text is converted to **lowercase**
3. The script loops through each keyword map **from top to bottom**
4. The **first keyword that appears anywhere in the text** wins — the loop stops immediately
5. If **no keyword matches**, the default value is used (`DEFAULT_PRIORITY` or `DEFAULT_CATEGORY`)
6. For Client Name, if no keyword matches, the value is set to `'Unknown'`

**Ordering tip:** Put your most specific / highest-priority keywords at the **top** of each map. For example, `'server down'` should come before `'down'` because `'server down'` is more specific.

---

## 11. Full Script

Access script. Groovy in this repo or copy the entire script below, make your changes in the CONFIG section, and paste it into ScriptRunner.

```groovy
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
```

---

## Quick Reference Checklist

Before saving the listener, confirm you have done all of the following:

- [ ] Replaced `DEMO` with your real project key
- [ ] Set `CLIENT_NAME_FIELD_ID` to your real field ID (with `L` at the end)
- [ ] Set `TICKET_CATEGORY_FIELD_ID` to your real field ID (with `L` at the end)
- [ ] Updated `CLIENT_KEYWORDS` with your real client names (right side matches dropdown exactly)
- [ ] Updated `PRIORITY_KEYWORDS` right-side values to match your project's valid priorities
- [ ] Updated `CATEGORY_KEYWORDS` right-side values to match your Ticket Category dropdown options
- [ ] Set `DEFAULT_PRIORITY` to a valid priority in your project
- [ ] Set `DEFAULT_CATEGORY` to a valid option in your Ticket Category field
- [ ] Created the listener with event set to **Issue Created**
- [ ] Tested with a real issue and checked the logs

---

*Built with [ScriptRunner for Jira Cloud](https://marketplace.atlassian.com/apps/6820/scriptrunner-for-jira) by Adaptavist.*
