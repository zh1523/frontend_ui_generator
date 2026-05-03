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
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
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
    $rect = New-Object System.Drawing.RectangleF($x, $y, $width, $height)
    $graphics.DrawString($text, $font, $brush, $rect, $format)
    $format.Dispose()
    $brush.Dispose()
    $font.Dispose()
}

function Draw-Panel(
    [System.Drawing.Graphics]$graphics,
    [float]$x,
    [float]$y,
    [float]$width,
    [float]$height,
    [string]$title,
    [string]$body,
    [string]$fill,
    [string]$border,
    [string]$titleFill = "#F4F8FC"
) {
    $fillBrush = New-Object System.Drawing.SolidBrush (New-Color $fill)
    $titleBrush = New-Object System.Drawing.SolidBrush (New-Color $titleFill)
    $pen = New-Object System.Drawing.Pen (New-Color $border), 3
    $graphics.FillRectangle($fillBrush, $x, $y, $width, $height)
    $graphics.DrawRectangle($pen, $x, $y, $width, $height)
    $graphics.FillRectangle($titleBrush, $x, $y, $width, 46)
    $graphics.DrawLine($pen, $x, $y + 46, $x + $width, $y + 46)
    Draw-Text -graphics $graphics -text $title -x ($x + 16) -y ($y + 10) -width ($width - 32) -height 22 -size 16 -style "Bold" -color "#17324A"
    if ($body) {
        Draw-Text -graphics $graphics -text $body -x ($x + 16) -y ($y + 58) -width ($width - 32) -height ($height - 68) -size 12 -style "Regular" -color "#3D4E61"
    }
    $pen.Dispose()
    $titleBrush.Dispose()
    $fillBrush.Dispose()
}

function Draw-Button(
    [System.Drawing.Graphics]$graphics,
    [float]$x,
    [float]$y,
    [float]$width,
    [float]$height,
    [string]$text,
    [string]$fill,
    [string]$border,
    [string]$textColor
) {
    $fillBrush = New-Object System.Drawing.SolidBrush (New-Color $fill)
    $pen = New-Object System.Drawing.Pen (New-Color $border), 2
    $graphics.FillRectangle($fillBrush, $x, $y, $width, $height)
    $graphics.DrawRectangle($pen, $x, $y, $width, $height)
    Draw-Text -graphics $graphics -text $text -x $x -y ($y + 10) -width $width -height 20 -size 12 -style "Bold" -color $textColor -align "Center"
    $pen.Dispose()
    $fillBrush.Dispose()
}

function Draw-Input(
    [System.Drawing.Graphics]$graphics,
    [float]$x,
    [float]$y,
    [float]$width,
    [float]$height,
    [string]$label,
    [string]$value
) {
    Draw-Text -graphics $graphics -text $label -x $x -y ($y - 22) -width $width -height 18 -size 11 -style "Bold" -color "#445468"
    Draw-Panel -graphics $graphics -x $x -y $y -width $width -height $height -title "" -body "" -fill "#FBFDFF" -border "#D7E4EF" -titleFill "#FBFDFF"
    Draw-Text -graphics $graphics -text $value -x ($x + 14) -y ($y + 14) -width ($width - 28) -height ($height - 20) -size 12 -style "Regular" -color "#23364A"
}

function Draw-Arrow(
    [System.Drawing.Graphics]$graphics,
    [float]$x1,
    [float]$y1,
    [float]$x2,
    [float]$y2,
    [string]$label = ""
) {
    $pen = New-Object System.Drawing.Pen (New-Color "#66788A"), 3
    $cap = New-Object System.Drawing.Drawing2D.AdjustableArrowCap(7, 9, $true)
    $pen.CustomEndCap = $cap
    $graphics.DrawLine($pen, $x1, $y1, $x2, $y2)
    if ($label) {
        $midX = [math]::Min($x1, $x2) + [math]::Abs($x2 - $x1) / 2
        $midY = [math]::Min($y1, $y2) + [math]::Abs($y2 - $y1) / 2
        Draw-Text -graphics $graphics -text $label -x ($midX - 70) -y ($midY - 16) -width 140 -height 22 -size 11 -style "Bold" -color "#516273" -align "Center"
    }
    $cap.Dispose()
    $pen.Dispose()
}

function Draw-Header([System.Drawing.Graphics]$graphics, [string]$title, [string]$subtitle) {
    Draw-Text -graphics $graphics -text $title -x 40 -y 24 -width 1200 -height 30 -size 25 -style "Bold" -color "#183B56"
    Draw-Text -graphics $graphics -text $subtitle -x 40 -y 60 -width 1500 -height 24 -size 12 -style "Regular" -color "#607082"
}

function Draw-GenerationFlow() {
    $canvas = New-Canvas 1800 940
    $g = $canvas.Graphics
    Draw-Header $g "Generation Task Flow" "Request validation, stream generation, code extraction, safety scan and persistence."

    Draw-Panel $g 70 130 250 110 "Step 1 Request" "GeneratorView submits prompt, component name and constraints." "#F7FBFF" "#86AEDA" "#EAF3FF"
    Draw-Panel $g 390 130 250 110 "Step 2 Create Task" "GenerationService verifies project and workspace, then saves a pending task." "#F9FCF6" "#8FB170" "#EDF7E7"
    Draw-Panel $g 710 130 250 110 "Step 3 Open SSE" "GenerationController returns SseEmitter for long-lived response." "#F7FBFF" "#86AEDA" "#EAF3FF"
    Draw-Panel $g 1030 130 250 110 "Step 4 Lock And Quota" "GenerationStreamService prevents duplicate runs and checks token quota." "#FFF9F1" "#D1A568" "#FFF0D7"
    Draw-Panel $g 1350 130 340 110 "Step 5 Cache Or Model" "Cache hit returns directly, otherwise Qwen client streams tokens from model service." "#FFF7FB" "#D289A6" "#FFEAF2"

    Draw-Panel $g 160 430 260 120 "Step 6 Token Stream" "Every token is pushed to browser so the page can update progressively." "#F7FBFF" "#86AEDA" "#EAF3FF"
    Draw-Panel $g 500 430 260 120 "Step 7 SFC Extract" "SfcCodeExtractor normalizes template, script and style sections." "#F9FCF6" "#8FB170" "#EDF7E7"
    Draw-Panel $g 840 430 260 120 "Step 8 Safety Scan" "CodeSafetyScanner blocks eval, remote import and network request patterns." "#FFF9F1" "#D1A568" "#FFF0D7"
    Draw-Panel $g 1180 430 260 120 "Step 9 Save Version" "Version content, safety level and compile flag are stored in database." "#F9FCF6" "#8FB170" "#EDF7E7"
    Draw-Panel $g 1520 430 210 120 "Step 10 UI Update" "Frontend receives final_code and refreshes code, preview and download entry." "#F7FBFF" "#86AEDA" "#EAF3FF"

    Draw-Arrow $g 320 185 390 185
    Draw-Arrow $g 640 185 710 185
    Draw-Arrow $g 960 185 1030 185
    Draw-Arrow $g 1280 185 1350 185
    Draw-Arrow $g 1520 240 290 430 "async"
    Draw-Arrow $g 420 490 500 490
    Draw-Arrow $g 760 490 840 490
    Draw-Arrow $g 1100 490 1180 490
    Draw-Arrow $g 1440 490 1520 490

    Save-Canvas $canvas "chapter5-flow-generation.png"
}

function Draw-PreviewFlow() {
    $canvas = New-Canvas 1800 920
    $g = $canvas.Graphics
    Draw-Header $g "Preview Runtime Flow" "Client-side preview parses the code again and constrains styles to the preview host."

    Draw-Panel $g 80 140 260 110 "Step 1 Read Version" "PreviewSandbox receives the latest Vue SFC string from generator page." "#F7FBFF" "#86AEDA" "#EAF3FF"
    Draw-Panel $g 410 140 280 110 "Step 2 Unsafe Check" "detectUnsafeCode rejects risky content before any runtime execution." "#FFF9F1" "#D1A568" "#FFF0D7"
    Draw-Panel $g 760 140 280 110 "Step 3 Parse Sections" "parseSfcSections splits template, script and style for later assembly." "#F9FCF6" "#8FB170" "#EDF7E7"
    Draw-Panel $g 1110 140 280 110 "Step 4 Build State" "Fallback state and fallback methods are generated from template expressions." "#F7FBFF" "#86AEDA" "#EAF3FF"
    Draw-Panel $g 1460 140 250 110 "Step 5 Scope CSS" "scopeCssToHost keeps generated styles inside preview-host." "#F9FCF6" "#8FB170" "#EDF7E7"

    Draw-Panel $g 250 430 320 120 "Step 6 Runtime Assemble" "Dynamic component merges script output, inferred state and preview helpers." "#F7FBFF" "#86AEDA" "#EAF3FF"
    Draw-Panel $g 690 430 320 120 "Step 7 Render Preview" "Vue mounts the temporary component and shows the visible result." "#F9FCF6" "#8FB170" "#EDF7E7"
    Draw-Panel $g 1130 430 260 120 "Step 8 Error Path" "Parser or runtime errors are converted into readable preview messages." "#FFF7FB" "#D289A6" "#FFEAF2"
    Draw-Panel $g 1460 430 250 120 "Step 9 Block Path" "Blocked code shows interception status instead of executing." "#FFF7FB" "#D289A6" "#FFEAF2"

    Draw-Arrow $g 340 195 410 195
    Draw-Arrow $g 690 195 760 195
    Draw-Arrow $g 1040 195 1110 195
    Draw-Arrow $g 1390 195 1460 195
    Draw-Arrow $g 1580 250 510 430 "allow"
    Draw-Arrow $g 570 490 690 490
    Draw-Arrow $g 1010 490 1130 490 "error"
    Draw-Arrow $g 690 250 1580 430 "blocked"

    Save-Canvas $canvas "chapter5-flow-preview.png"
}

function Draw-GeneratorUi() {
    $canvas = New-Canvas 1800 1080
    $g = $canvas.Graphics
    Draw-Header $g "Generator Interface Screenshot" "Main page integrates input form, stream text, code panel and preview area."

    Draw-Panel $g 50 90 1700 64 "" "" "#FFFFFF" "#D7E4EF" "#FFFFFF"
    Draw-Text -graphics $g -text "UI Component Generator" -x 76 -y 110 -width 360 -height 24 -size 20 -style "Bold" -color "#1E2D3D"
    Draw-Button $g 1210 104 170 34 "Project: Default" "#F6FAFE" "#D7E4EF" "#334155"
    Draw-Button $g 1400 104 120 34 "Token 18240" "#F6FAFE" "#D7E4EF" "#334155"
    Draw-Button $g 1540 104 120 34 "admin" "#F6FAFE" "#D7E4EF" "#334155"

    Draw-Panel $g 60 176 620 844 "Describe Component" "" "#FFFFFF" "#D7E4EF" "#F5F9FD"
    Draw-Input $g 90 252 560 54 "Component Name" "UserTable"
    Draw-Input $g 90 350 560 126 "Prompt" "A searchable user table with pagination and status tags."
    Draw-Input $g 90 542 560 86 "Constraints (JSON)" "{""theme"":""light"",""language"":""zh-CN""}"
    Draw-Text -graphics $g -text "Demo Data" -x 90 -y 662 -width 120 -height 18 -size 11 -style "Bold" -color "#445468"
    Draw-Panel $g 90 684 560 56 "" "" "#FBFDFF" "#D7E4EF" "#FBFDFF"
    Draw-Text -graphics $g -text "Enabled to make preview content visible immediately." -x 110 -y 702 -width 520 -height 18 -size 12 -style "Regular" -color "#3D4E61"

    Draw-Button $g 90 770 120 42 "Generate" "#2F7BFF" "#2F7BFF" "#FFFFFF"
    Draw-Button $g 228 770 136 42 "Regenerate" "#FFFFFF" "#D7E4EF" "#334155"
    Draw-Button $g 382 770 160 42 "Download .vue" "#FFFFFF" "#D7E4EF" "#334155"

    Draw-Panel $g 90 838 560 60 "" "" "#EEF6FF" "#CADCEF" "#EEF6FF"
    Draw-Text -graphics $g -text "Task #21 - SUCCEEDED" -x 110 -y 859 -width 260 -height 18 -size 12 -style "Bold" -color "#24405A"

    Draw-Panel $g 90 920 560 80 "Streaming Output" "Generating template, script and style from prompt..." "#F9FCFF" "#D7E4EF" "#EEF5FB"

    Draw-Panel $g 718 176 1020 392 "Code Editor" "" "#FFFFFF" "#D7E4EF" "#F5F9FD"
    Draw-Text -graphics $g -text "<template>" -x 748 -y 246 -width 300 -height 20 -size 12 -style "Regular" -color "#24384B"
    Draw-Text -graphics $g -text "  <el-table :data=""users"">" -x 748 -y 276 -width 420 -height 20 -size 12 -style "Regular" -color "#24384B"
    Draw-Text -graphics $g -text "    <el-table-column prop=""name"" />" -x 748 -y 306 -width 420 -height 20 -size 12 -style "Regular" -color "#24384B"
    Draw-Text -graphics $g -text "  </el-table>" -x 748 -y 336 -width 300 -height 20 -size 12 -style "Regular" -color "#24384B"
    Draw-Text -graphics $g -text "</template>" -x 748 -y 366 -width 300 -height 20 -size 12 -style "Regular" -color "#24384B"
    Draw-Text -graphics $g -text "<script setup>" -x 748 -y 426 -width 300 -height 20 -size 12 -style "Regular" -color "#24384B"
    Draw-Text -graphics $g -text "const users = ref([{ name: 'admin', role: 'ADMIN' }]);" -x 748 -y 456 -width 600 -height 20 -size 12 -style "Regular" -color "#24384B"

    Draw-Panel $g 718 602 1020 418 "Preview Area" "" "#FFFFFF" "#D7E4EF" "#F5F9FD"
    Draw-Panel $g 748 670 960 300 "" "" "#F9FCFF" "#C0D8EC" "#F9FCFF"
    Draw-Button $g 780 710 180 38 "Search keyword" "#FFFFFF" "#D7E4EF" "#7A8896"
    Draw-Button $g 980 710 88 38 "Query" "#2F7BFF" "#2F7BFF" "#FFFFFF"
    Draw-Panel $g 780 774 840 146 "" "" "#FFFFFF" "#D7E4EF" "#FFFFFF"
    Draw-Text -graphics $g -text "ID        Username      Role       Status" -x 804 -y 794 -width 420 -height 18 -size 12 -style "Bold" -color "#334155"
    Draw-Text -graphics $g -text "1         admin         ADMIN      Enabled" -x 804 -y 828 -width 420 -height 18 -size 12 -style "Regular" -color "#334155"
    Draw-Text -graphics $g -text "2         demo          USER       Enabled" -x 804 -y 860 -width 420 -height 18 -size 12 -style "Regular" -color "#334155"
    Draw-Text -graphics $g -text "3         test          USER       Disabled" -x 804 -y 892 -width 420 -height 18 -size 12 -style "Regular" -color "#334155"

    Save-Canvas $canvas "chapter5-ui-generator.png"
}

function Draw-ManagementUi() {
    $canvas = New-Canvas 1800 1120
    $g = $canvas.Graphics
    Draw-Header $g "Login, History And Admin Screens" "The thesis uses a combined figure to present the other major operation pages."

    Draw-Panel $g 56 98 500 960 "Login Page" "" "#FFFFFF" "#D7E4EF" "#F5F9FD"
    Draw-Text -graphics $g -text "UI Component Generator Login" -x 118 -y 170 -width 380 -height 24 -size 20 -style "Bold" -color "#203243"
    Draw-Button $g 130 236 150 34 "Login" "#EEF5FB" "#D7E4EF" "#334155"
    Draw-Button $g 290 236 150 34 "Register" "#FFFFFF" "#D7E4EF" "#334155"
    Draw-Input $g 130 356 352 54 "Username" "admin"
    Draw-Input $g 130 454 352 54 "Password" "********"
    Draw-Button $g 130 550 352 46 "Submit" "#2F7BFF" "#2F7BFF" "#FFFFFF"

    Draw-Panel $g 648 98 528 960 "History Page" "" "#FFFFFF" "#D7E4EF" "#F5F9FD"
    Draw-Text -graphics $g -text "History Versions" -x 680 -y 166 -width 220 -height 24 -size 20 -style "Bold" -color "#203243"
    Draw-Panel $g 680 226 460 54 "" "" "#F6FAFE" "#D7E4EF" "#F6FAFE"
    Draw-Text -graphics $g -text "Task list and version list are displayed side by side." -x 700 -y 246 -width 420 -height 18 -size 12 -style "Regular" -color "#3D4E61"
    Draw-Panel $g 680 308 212 610 "Task List" "" "#FFFFFF" "#D7E4EF" "#EEF5FB"
    Draw-Panel $g 928 308 212 610 "Version List" "" "#FFFFFF" "#D7E4EF" "#EEF5FB"
    Draw-Text -graphics $g -text "TaskID  Name        Status" -x 700 -y 368 -width 180 -height 18 -size 12 -style "Bold" -color "#334155"
    Draw-Text -graphics $g -text "21      UserTable   SUCCEEDED" -x 700 -y 404 -width 190 -height 18 -size 12 -style "Regular" -color "#334155"
    Draw-Text -graphics $g -text "22      ProductCard FAILED" -x 700 -y 436 -width 190 -height 18 -size 12 -style "Regular" -color "#334155"
    Draw-Text -graphics $g -text "Version  Safety  Action" -x 948 -y 368 -width 180 -height 18 -size 12 -style "Bold" -color "#334155"
    Draw-Text -graphics $g -text "v3      SAFE    Open / Download" -x 948 -y 404 -width 190 -height 18 -size 12 -style "Regular" -color "#334155"
    Draw-Text -graphics $g -text "v2      SAFE    Open / Download" -x 948 -y 436 -width 190 -height 18 -size 12 -style "Regular" -color "#334155"

    Draw-Panel $g 1266 98 478 960 "Admin Page" "" "#FFFFFF" "#D7E4EF" "#F5F9FD"
    Draw-Text -graphics $g -text "User Permission Admin" -x 1298 -y 166 -width 260 -height 24 -size 20 -style "Bold" -color "#203243"
    Draw-Button $g 1298 226 286 40 "Search by username" "#FFFFFF" "#D7E4EF" "#7A8896"
    Draw-Button $g 1602 226 110 40 "Query" "#2F7BFF" "#2F7BFF" "#FFFFFF"
    Draw-Panel $g 1298 298 414 604 "" "" "#FFFFFF" "#D7E4EF" "#FFFFFF"
    Draw-Text -graphics $g -text "UserID  Username  Role   Status   Last Login" -x 1318 -y 330 -width 380 -height 18 -size 12 -style "Bold" -color "#334155"
    Draw-Text -graphics $g -text "1       admin    ADMIN  Enabled  2026-04-24 18:32" -x 1318 -y 366 -width 390 -height 18 -size 12 -style "Regular" -color "#334155"
    Draw-Text -graphics $g -text "2       demo     USER   Enabled  2026-04-24 17:05" -x 1318 -y 400 -width 390 -height 18 -size 12 -style "Regular" -color "#334155"
    Draw-Text -graphics $g -text "3       test     USER   Disabled 2026-04-23 22:18" -x 1318 -y 434 -width 390 -height 18 -size 12 -style "Regular" -color "#334155"

    Save-Canvas $canvas "chapter5-ui-management.png"
}

New-Item -ItemType Directory -Force -Path $figureDir | Out-Null

Draw-GenerationFlow
Draw-PreviewFlow
Draw-GeneratorUi
Draw-ManagementUi
