$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$figureDir = Join-Path $PSScriptRoot "..\figures"

function New-Color([string]$hex) {
    [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function New-Font([float]$size, [string]$style = "Regular") {
    New-Object System.Drawing.Font("Segoe UI", $size, [System.Drawing.FontStyle]::$style)
}

function New-Canvas([int]$width, [int]$height) {
    $bitmap = New-Object System.Drawing.Bitmap($width, $height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $graphics.Clear((New-Color "#FFFFFF"))
    @{
        Bitmap = $bitmap
        Graphics = $graphics
    }
}

function Save-Canvas($canvas, [string]$name) {
    $path = Join-Path $figureDir $name
    $canvas.Bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $canvas.Graphics.Dispose()
    $canvas.Bitmap.Dispose()
}

function Draw-Text(
    [System.Drawing.Graphics]$graphics,
    [string]$text,
    [float]$x,
    [float]$y,
    [float]$width,
    [float]$height,
    [float]$size,
    [string]$style,
    [string]$color,
    [string]$align = "Left"
) {
    $font = New-Font $size $style
    $brush = New-Object System.Drawing.SolidBrush (New-Color $color)
    $format = New-Object System.Drawing.StringFormat
    $format.LineAlignment = [System.Drawing.StringAlignment]::Near
    switch ($align) {
        "Center" { $format.Alignment = [System.Drawing.StringAlignment]::Center }
        "Right" { $format.Alignment = [System.Drawing.StringAlignment]::Far }
        default { $format.Alignment = [System.Drawing.StringAlignment]::Near }
    }
    $graphics.DrawString($text, $font, $brush, (New-Object System.Drawing.RectangleF($x, $y, $width, $height)), $format)
    $format.Dispose()
    $brush.Dispose()
    $font.Dispose()
}

function Draw-Box(
    [System.Drawing.Graphics]$graphics,
    [float]$x,
    [float]$y,
    [float]$width,
    [float]$height,
    [string]$title,
    [string]$content,
    [string]$fill,
    [string]$border,
    [string]$titleFill = $null
) {
    $fillBrush = New-Object System.Drawing.SolidBrush (New-Color $fill)
    $pen = New-Object System.Drawing.Pen (New-Color $border), 3
    $graphics.FillRectangle($fillBrush, $x, $y, $width, $height)
    $graphics.DrawRectangle($pen, $x, $y, $width, $height)
    if ($titleFill) {
        $titleBrush = New-Object System.Drawing.SolidBrush (New-Color $titleFill)
        $graphics.FillRectangle($titleBrush, $x, $y, $width, 52)
        $graphics.DrawLine($pen, $x, $y + 52, $x + $width, $y + 52)
        $titleBrush.Dispose()
        Draw-Text $graphics $title ($x + 14) ($y + 10) ($width - 28) 28 18 "Bold" "#17324A"
        Draw-Text $graphics $content ($x + 14) ($y + 62) ($width - 28) ($height - 72) 14 "Regular" "#344054"
    } else {
        Draw-Text $graphics $title ($x + 14) ($y + 10) ($width - 28) 28 18 "Bold" "#17324A"
        Draw-Text $graphics $content ($x + 14) ($y + 44) ($width - 28) ($height - 54) 14 "Regular" "#344054"
    }
    $pen.Dispose()
    $fillBrush.Dispose()
}

function Draw-Arrow(
    [System.Drawing.Graphics]$graphics,
    [float]$x1,
    [float]$y1,
    [float]$x2,
    [float]$y2,
    [string]$label = "",
    [string]$color = "#61758A"
) {
    $pen = New-Object System.Drawing.Pen (New-Color $color), 3
    $cap = New-Object System.Drawing.Drawing2D.AdjustableArrowCap(6, 8, $true)
    $pen.CustomEndCap = $cap
    $graphics.DrawLine($pen, $x1, $y1, $x2, $y2)
    if ($label) {
        $midX = [math]::Min($x1, $x2) + [math]::Abs($x2 - $x1) / 2
        $midY = [math]::Min($y1, $y2) + [math]::Abs($y2 - $y1) / 2
        Draw-Text $graphics $label ($midX - 70) ($midY - 15) 140 24 12 "Bold" "#445468" "Center"
    }
    $cap.Dispose()
    $pen.Dispose()
}

function Draw-Entity(
    [System.Drawing.Graphics]$graphics,
    [float]$x,
    [float]$y,
    [float]$width,
    [float]$height,
    [string]$title,
    [string[]]$fields
) {
    Draw-Box $graphics $x $y $width $height $title ($fields -join "`n") "#F8FBFF" "#7AA6D8" "#EAF4FF"
}

function Draw-Header([System.Drawing.Graphics]$graphics, [string]$text) {
    Draw-Text $graphics $text 40 24 1720 36 26 "Bold" "#183B56"
}

function Draw-LayerArchitecture() {
    $canvas = New-Canvas 1800 1100
    $g = $canvas.Graphics
    Draw-Header $g "System Layer Architecture"

    Draw-Box $g 80 100 1640 250 "Presentation Layer" "Browser side pages and interaction logic" "#F5FAFF" "#82AED6" "#E8F3FF"
    Draw-Box $g 120 170 260 120 "Login Page" "register / login`nidentity entry" "#FFFFFF" "#82AED6"
    Draw-Box $g 420 170 300 120 "Generator Page" "prompt form`nstream text`ncode panel`npreview panel" "#FFFFFF" "#82AED6"
    Draw-Box $g 760 170 300 120 "History Page" "task list`nversion list`nopen / download" "#FFFFFF" "#82AED6"
    Draw-Box $g 1100 170 280 120 "Admin Page" "user search`nrole update`nstatus update" "#FFFFFF" "#82AED6"
    Draw-Box $g 1420 170 260 120 "State And Router" "Vue Router`nPinia stores`nrequest wrapper" "#FFFFFF" "#82AED6"

    Draw-Box $g 80 410 1640 360 "Business Layer" "Backend controllers, services and common components" "#F9FCF7" "#8BB174" "#EDF7E8"
    Draw-Box $g 120 485 250 120 "Controller Group" "AuthController`nProjectController`nGenerationController`nComponentVersionController`nAdminController" "#FFFFFF" "#8BB174"
    Draw-Box $g 410 485 260 120 "Auth And Permission" "AuthInterceptor`nAuthService`nAuthorizationService" "#FFFFFF" "#8BB174"
    Draw-Box $g 710 485 250 120 "Project Context" "WorkspaceService`nProjectService" "#FFFFFF" "#8BB174"
    Draw-Box $g 1000 485 300 120 "Generation Core" "GenerationService`nGenerationStreamService`nQwenLlmClient" "#FFFFFF" "#8BB174"
    Draw-Box $g 1340 485 250 120 "Version And Admin" "ComponentVersionService`nAdminUserService`nCostControlService" "#FFFFFF" "#8BB174"
    Draw-Box $g 320 635 260 90 "Safety And Parsing" "SfcCodeExtractor`nCodeSafetyScanner" "#FFFFFF" "#8BB174"
    Draw-Box $g 640 635 260 90 "Cache And Limit" "GenerationCacheService`nGenerationRateLimiter" "#FFFFFF" "#8BB174"
    Draw-Box $g 960 635 260 90 "Logs And Metrics" "LlmCallLogRepository`nusage statistics" "#FFFFFF" "#8BB174"
    Draw-Box $g 1280 635 260 90 "External Model Call" "DashScope / Qwen`nstream request" "#FFFFFF" "#8BB174"

    Draw-Box $g 80 840 1640 180 "Data Layer" "Persistent entities and repositories in MySQL" "#FFF9F2" "#D2A867" "#FFF0D9"
    Draw-Box $g 120 895 250 90 "User Data" "app_user`nuser_session" "#FFFFFF" "#D2A867"
    Draw-Box $g 420 895 250 90 "Project Data" "workspace`nproject_space" "#FFFFFF" "#D2A867"
    Draw-Box $g 720 895 300 90 "Generation Data" "generation_task`ncomponent_version" "#FFFFFF" "#D2A867"
    Draw-Box $g 1070 895 250 90 "Usage Data" "llm_call_log" "#FFFFFF" "#D2A867"
    Draw-Box $g 1370 895 250 90 "Audit Data" "audit_log" "#FFFFFF" "#D2A867"

    Draw-Arrow $g 900 350 900 410 "HTTP / SSE"
    Draw-Arrow $g 900 770 900 840 "JPA"

    Save-Canvas $canvas "chapter4-layer-architecture.png"
}

function Draw-TechArchitecture() {
    $canvas = New-Canvas 1800 1080
    $g = $canvas.Graphics
    Draw-Header $g "Technical Architecture And Component Interaction"

    Draw-Box $g 60 140 240 120 "1 Browser Request" "user submits login, project or generation request" "#F5FAFF" "#82AED6" "#E8F3FF"
    Draw-Box $g 350 140 280 120 "2 Frontend Handling" "router guard`nPinia state`nrequest headers`nfetch / SSE setup" "#F5FAFF" "#82AED6" "#E8F3FF"
    Draw-Box $g 680 140 240 120 "3 Controller Entry" "AuthController`nProjectController`nGenerationController" "#F9FCF7" "#8BB174" "#EDF7E8"
    Draw-Box $g 970 140 240 120 "4 Identity Check" "AuthInterceptor`nAuthService`nproject ownership check" "#F9FCF7" "#8BB174" "#EDF7E8"
    Draw-Box $g 1260 140 240 120 "5 Task Creation" "GenerationService`ncreate task`nset PENDING" "#F9FCF7" "#8BB174" "#EDF7E8"

    Draw-Box $g 170 400 260 120 "6 Stream Start" "open SSE`nreturn started event" "#F5FAFF" "#82AED6" "#E8F3FF"
    Draw-Box $g 500 400 280 120 "7 Generation Flow" "GenerationStreamService`nmark GENERATING`ncheck quota / cache" "#F9FCF7" "#8BB174" "#EDF7E8"
    Draw-Box $g 850 400 260 120 "8 Model Call" "QwenLlmClient`nstream token chunks" "#FFF6FA" "#D78CA8" "#FFE7F1"
    Draw-Box $g 1180 400 260 120 "9 Parse And Safety" "SfcCodeExtractor`nCodeSafetyScanner" "#F9FCF7" "#8BB174" "#EDF7E8"
    Draw-Box $g 1510 400 220 120 "10 Persist Result" "save version`nsave call log`nmark SUCCEEDED or FAILED" "#FFF9F2" "#D2A867" "#FFF0D9"

    Draw-Box $g 280 700 280 120 "11 Database Read Write" "users`nprojects`ntasks`nversions`nlogs`naudits" "#FFF9F2" "#D2A867" "#FFF0D9"
    Draw-Box $g 650 700 280 120 "12 Response To Frontend" "token events`nfinal_code`nerror / done" "#F5FAFF" "#82AED6" "#E8F3FF"
    Draw-Box $g 1020 700 300 120 "13 Frontend Presentation" "update stream text`nshow code`nrender preview`nallow download" "#F5FAFF" "#82AED6" "#E8F3FF"
    Draw-Box $g 1410 700 260 120 "14 User Gets Result" "preview component`nopen history`ndownload .vue" "#F5FAFF" "#82AED6" "#E8F3FF"

    Draw-Arrow $g 300 200 350 200
    Draw-Arrow $g 630 200 680 200
    Draw-Arrow $g 920 200 970 200
    Draw-Arrow $g 1210 200 1260 200
    Draw-Arrow $g 1380 260 1380 400
    Draw-Arrow $g 430 460 500 460
    Draw-Arrow $g 780 460 850 460
    Draw-Arrow $g 1110 460 1180 460
    Draw-Arrow $g 1440 460 1510 460
    Draw-Arrow $g 1620 520 1620 700
    Draw-Arrow $g 1510 760 1320 760
    Draw-Arrow $g 930 760 1020 760
    Draw-Arrow $g 1320 760 1410 760
    Draw-Arrow $g 1510 520 560 760 "db + events"
    Draw-Arrow $g 560 760 650 760
    Draw-Arrow $g 560 700 560 520 "persist"

    Save-Canvas $canvas "chapter4-tech-architecture.png"
}

function Draw-ModuleInteraction() {
    $canvas = New-Canvas 1800 1040
    $g = $canvas.Graphics
    Draw-Header $g "Core Modules And Interface Relations"

    Draw-Box $g 80 120 360 820 "Frontend Calls" "" "#F5FAFF" "#82AED6" "#E8F3FF"
    Draw-Box $g 120 200 280 96 "Auth Module" "register / login / logout / me" "#FFFFFF" "#82AED6"
    Draw-Box $g 120 330 280 120 "Generation Module" "createGenerationTask`nstreamGenerationTask`nregenerateGenerationTask" "#FFFFFF" "#82AED6"
    Draw-Box $g 120 490 280 120 "History Module" "listWorkspaceTasks`nlistTaskVersions`ngetVersionDetail`ndownloadVersion" "#FFFFFF" "#82AED6"
    Draw-Box $g 120 650 280 96 "Project And Cost" "listProjects / createProject`ngetCostUsage" "#FFFFFF" "#82AED6"
    Draw-Box $g 120 780 280 96 "Admin Module" "listAdminUsers`nupdateAdminUserRole`nupdateAdminUserStatus" "#FFFFFF" "#82AED6"

    Draw-Box $g 540 120 300 820 "API Resources" "" "#F9FCF7" "#8BB174" "#EDF7E8"
    Draw-Box $g 580 200 220 88 "/auth" "auth endpoints" "#FFFFFF" "#8BB174"
    Draw-Box $g 580 320 220 88 "/projects" "project endpoints" "#FFFFFF" "#8BB174"
    Draw-Box $g 580 440 220 88 "/generations" "create / detail / retry / SSE" "#FFFFFF" "#8BB174"
    Draw-Box $g 580 560 220 88 "/versions" "detail / download" "#FFFFFF" "#8BB174"
    Draw-Box $g 580 680 220 88 "/cost" "usage query" "#FFFFFF" "#8BB174"
    Draw-Box $g 580 800 220 88 "/admin" "user governance" "#FFFFFF" "#8BB174"

    Draw-Box $g 940 120 360 820 "Backend Modules" "" "#FFF9F2" "#D2A867" "#FFF0D9"
    Draw-Box $g 980 190 280 110 "Identity" "AuthService`nAuthInterceptor`nAuthorizationService" "#FFFFFF" "#D2A867"
    Draw-Box $g 980 330 280 110 "Project Context" "ProjectService`nWorkspaceService" "#FFFFFF" "#D2A867"
    Draw-Box $g 980 470 280 150 "Generation Pipeline" "GenerationService`nGenerationStreamService`nQwenLlmClient`nSfcCodeExtractor`nCodeSafetyScanner" "#FFFFFF" "#D2A867"
    Draw-Box $g 980 660 280 110 "Version Flow" "ComponentVersionService" "#FFFFFF" "#D2A867"
    Draw-Box $g 980 800 280 110 "Cost And Admin" "CostControlService`nAdminUserService" "#FFFFFF" "#D2A867"

    Draw-Box $g 1400 200 280 180 "Persistence And Logs" "AppUser`nUserSession`nProjectSpace`nGenerationTask`nComponentVersion`nLlmCallLog`nAuditLog" "#FFF6FA" "#D78CA8" "#FFE7F1"
    Draw-Box $g 1400 470 280 130 "Model Service" "Qwen streaming output`ntoken and usage" "#FFF6FA" "#D78CA8" "#FFE7F1"
    Draw-Box $g 1400 680 280 130 "Preview Sandbox" "detectUnsafeCode`nruntime render and style isolation" "#FFF6FA" "#D78CA8" "#FFE7F1"

    Draw-Arrow $g 400 248 580 244 "JSON"
    Draw-Arrow $g 400 390 580 484 "POST + SSE"
    Draw-Arrow $g 400 548 580 604 "GET"
    Draw-Arrow $g 400 698 580 364 "JSON"
    Draw-Arrow $g 400 826 580 846 "PATCH"
    Draw-Arrow $g 800 244 980 244
    Draw-Arrow $g 800 364 980 384
    Draw-Arrow $g 800 484 980 544
    Draw-Arrow $g 800 604 980 714
    Draw-Arrow $g 800 724 980 854
    Draw-Arrow $g 1260 300 1400 300 "JPA"
    Draw-Arrow $g 1260 544 1400 534 "HTTPS"
    Draw-Arrow $g 1260 714 1400 744 "code"

    Save-Canvas $canvas "chapter4-module-interaction.png"
}

function Draw-ErDiagram() {
    $canvas = New-Canvas 1800 1220
    $g = $canvas.Graphics
    Draw-Header $g "Core Entity Relationship Diagram"

    Draw-Entity $g 60 120 300 210 "app_user" @(
        "PK id",
        "username",
        "password_hash",
        "role / enabled",
        "created_at / updated_at",
        "last_login_at"
    )
    Draw-Entity $g 430 120 300 190 "user_session" @(
        "PK id",
        "FK user_id",
        "token",
        "expires_at",
        "last_active_at"
    )
    Draw-Entity $g 60 430 300 210 "workspace" @(
        "PK id",
        "workspace_key",
        "ip_hash / ua_hash",
        "FK owner_user_id",
        "status",
        "created_at / last_active_at"
    )
    Draw-Entity $g 430 430 300 220 "project_space" @(
        "PK id",
        "FK owner_user_id",
        "FK workspace_id (unique)",
        "name / description",
        "archived",
        "created_at / updated_at"
    )
    Draw-Entity $g 820 430 340 260 "generation_task" @(
        "PK id",
        "FK workspace_id",
        "FK project_id",
        "component_name / prompt",
        "constraints_json",
        "include_demo_data / model",
        "status / error_message",
        "started_at / finished_at / created_at"
    )
    Draw-Entity $g 1250 300 320 240 "component_version" @(
        "PK id",
        "FK task_id",
        "version_no",
        "vue_code / template_code",
        "script_code / style_code",
        "safety_level / safety_reason",
        "compile_ok / download_count",
        "created_at"
    )
    Draw-Entity $g 1250 620 320 190 "llm_call_log" @(
        "PK id",
        "FK task_id",
        "provider / model",
        "request_tokens / response_tokens",
        "total_tokens / estimated_cost_usd",
        "latency_ms / finish_reason / created_at"
    )
    Draw-Entity $g 430 860 300 180 "audit_log" @(
        "PK id",
        "FK actor_user_id",
        "FK target_user_id",
        "action / detail",
        "created_at"
    )

    Draw-Arrow $g 360 215 430 215
    Draw-Text $g "1" 342 186 24 20 12 "Bold" "#5C6B7A"
    Draw-Text $g "N" 438 186 24 20 12 "Bold" "#5C6B7A"

    Draw-Arrow $g 210 330 210 430
    Draw-Text $g "1" 182 326 24 20 12 "Bold" "#5C6B7A"
    Draw-Text $g "N" 182 406 24 20 12 "Bold" "#5C6B7A"

    Draw-Arrow $g 360 520 430 520
    Draw-Text $g "1" 342 492 24 20 12 "Bold" "#5C6B7A"
    Draw-Text $g "1" 438 492 24 20 12 "Bold" "#5C6B7A"

    Draw-Arrow $g 730 540 820 540
    Draw-Text $g "1" 710 512 24 20 12 "Bold" "#5C6B7A"
    Draw-Text $g "N" 826 512 24 20 12 "Bold" "#5C6B7A"

    Draw-Arrow $g 1160 500 1250 420
    Draw-Text $g "1" 1146 470 24 20 12 "Bold" "#5C6B7A"
    Draw-Text $g "N" 1232 404 24 20 12 "Bold" "#5C6B7A"

    Draw-Arrow $g 1160 620 1250 700
    Draw-Text $g "1" 1146 642 24 20 12 "Bold" "#5C6B7A"
    Draw-Text $g "N" 1232 696 24 20 12 "Bold" "#5C6B7A"

    Draw-Arrow $g 210 640 210 950
    Draw-Arrow $g 360 950 430 950
    Draw-Text $g "1" 182 642 24 20 12 "Bold" "#5C6B7A"
    Draw-Text $g "N" 182 922 24 20 12 "Bold" "#5C6B7A"
    Draw-Text $g "1" 342 922 24 20 12 "Bold" "#5C6B7A"
    Draw-Text $g "N" 438 922 24 20 12 "Bold" "#5C6B7A"

    Save-Canvas $canvas "chapter4-er-diagram.png"
}

function Draw-GeneratorWireframe() {
    $canvas = New-Canvas 1800 980
    $g = $canvas.Graphics
    Draw-Header $g "Generator Workspace Wireframe"

    Draw-Box $g 60 90 1680 70 "Top Navigation" "system title | menus | project switch | token usage | theme toggle | user dropdown" "#F7FAFC" "#A8B7C9" "#EEF4F8"
    Draw-Box $g 60 190 620 700 "Left Input Area" "" "#FBFDFF" "#A8B7C9" "#F3F7FA"
    Draw-Box $g 100 250 540 80 "Component Name" "componentName" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 100 360 540 170 "Prompt Textarea" "natural language requirement, up to 2000 characters" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 100 560 540 110 "Constraints JSON" "constraints" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 100 700 540 70 "Demo Data Switch" "includeDemoData" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 100 790 540 60 "Action Buttons" "generate | regenerate | download .vue" "#FFFFFF" "#A8B7C9"

    Draw-Box $g 740 190 1000 320 "Code Panel" "CodeEditorPanel`nshows the full Vue SFC source returned by the backend." "#FBFDFF" "#A8B7C9" "#F3F7FA"
    Draw-Box $g 740 550 1000 340 "Preview Panel" "PreviewSandbox`nchecks unsafe code and renders template, script and style in a scoped host container." "#FBFDFF" "#A8B7C9" "#F3F7FA"
    Draw-Box $g 100 870 540 20 "" "" "#A8B7C9" "#A8B7C9"
    Draw-Text $g "Left side handles requirement input and task status. Right side shows source code and runtime preview." 740 900 960 40 16 "Regular" "#4A5968"

    Save-Canvas $canvas "chapter4-wireframe-generator.png"
}

function Draw-ManagementWireframe() {
    $canvas = New-Canvas 1800 980
    $g = $canvas.Graphics
    Draw-Header $g "Login, History And Admin Wireframes"

    Draw-Box $g 60 110 460 780 "Login Page" "" "#FBFDFF" "#A8B7C9" "#F3F7FA"
    Draw-Box $g 140 220 300 90 "Header" "system name + login/register tabs" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 140 360 300 90 "Username Input" "username" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 140 490 300 90 "Password Input" "password" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 140 620 300 70 "Submit Button" "login or register-and-login" "#FFFFFF" "#A8B7C9"

    Draw-Box $g 610 110 550 780 "History Page" "" "#FBFDFF" "#A8B7C9" "#F3F7FA"
    Draw-Box $g 650 210 470 90 "Page Intro" "task list + version list in a two-column layout" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 650 340 210 440 "Task List" "task id`ncomponent name`nstatus`nlatest version`nregenerate" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 910 340 210 440 "Version List" "version no`nsafety level`nstructure flag`ncreated time`nopen/download" "#FFFFFF" "#A8B7C9"

    Draw-Box $g 1250 110 490 780 "Admin Page" "" "#FBFDFF" "#A8B7C9" "#F3F7FA"
    Draw-Box $g 1290 210 410 90 "Search Toolbar" "keyword input + query button" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 1290 340 410 440 "User Table" "user id`nusername`nrole`nstatus`nlast login`nrole/status actions" "#FFFFFF" "#A8B7C9"
    Draw-Box $g 1290 820 410 70 "Pagination" "total | pager | page size" "#FFFFFF" "#A8B7C9"

    Save-Canvas $canvas "chapter4-wireframe-management.png"
}

New-Item -ItemType Directory -Force -Path $figureDir | Out-Null

Draw-LayerArchitecture
Draw-TechArchitecture
Draw-ModuleInteraction
Draw-ErDiagram
Draw-GeneratorWireframe
Draw-ManagementWireframe
