<#
    Villager Chess — trailer builder

    Assembles publishing/trailer/villager-chess-trailer.mp4 from the two recorded clips
    plus the six gallery screenshots. Silent by design (neither clip has an audio stream);
    the story is told with burned-in captions.

    Re-cutting the trailer means editing the TUNABLES block below and running this again.
    Nothing here is destructive: _build/ is scratch and is rebuilt from source every run.

    Requires ffmpeg on PATH (libfreetype build, for drawtext).
#>

$ErrorActionPreference = 'Stop'

$Root        = Split-Path -Parent $PSScriptRoot          # publishing/
$Trailer     = $PSScriptRoot                             # publishing/trailer/
$Shots       = Join-Path $Root 'screenshots'
$Build       = Join-Path $Trailer '_build'
$OutFile     = Join-Path $Trailer 'villager-chess-trailer.mp4'

# ----------------------------------------------------------------------------------
# TUNABLES — everything you would want to change lives here
# ----------------------------------------------------------------------------------

# Framing. The in-game GUI occupies roughly the middle third of a 1920-wide frame, so the
# UI beats punch in 1.5x. This crop was measured against sampled frames: it contains the
# trade screen, the Play Chess button, the board, the villager panel and the move list.
$PUNCH = 'crop=1280:720:320:144,scale=1920:1080:flags=lanczos'
# Full frame: sources are 1920x1008/1009, so scale to 1080 tall and centre-crop to width.
$FULL  = 'scale=-2:1080:flags=lanczos,crop=1920:1080'

# Cut points in seconds. seg1/seg2 are effectively variable-rate (605 frames across
# 13.98s despite a 60fps header), so every beat is re-encoded to 30fps CFR.
$S1_APPROACH_IN  = 0.0;  $S1_APPROACH_DUR = 4.4    # walking up to the librarian
$S1_TRADE_IN     = 4.4;  $S1_TRADE_DUR    = 3.0    # trade screen + Play Chess button
$S1_BOARD_IN     = 7.4;  $S1_BOARD_DUR    = 6.5    # board opens, first move
$S2_MOVES_IN     = 0.5;  $S2_MOVES_DUR    = 12.5   # move montage (sped up below)
$S2_SPEED        = 0.85                            # <1 is faster; 12.5s -> ~10.6s

# Still durations
$D_TITLE = 2.5; $D_CHECK = 3.5; $D_PROMO = 3.5; $D_MASTER = 3.5; $D_WIN = 4.0; $D_END = 5.0

# Captions
$CAP_APPROACH = 'Walk up to any villager'
$CAP_TRADE    = "Right-click " + [char]0x2192 + " Play Chess"
$CAP_BOARD    = 'A real chess engine, in the trade screen'
$CAP_MOVES    = 'Click to move ' + [char]0x00B7 + ' legal moves highlighted'
$CAP_CHECK    = "It knows when you're in check"
$CAP_PROMO    = 'Promote to anything'
$CAP_MASTER   = 'Level 5 villagers play at Master strength'
$CAP_WIN      = 'Beat them if you can'

$TITLE_MAIN = 'VILLAGER CHESS'
$TITLE_SUB  = 'Fabric ' + [char]0x00B7 + ' Minecraft 26.2'
$END_SUB    = 'Modrinth ' + [char]0x00B7 + ' CurseForge'

$FONT_FILE  = 'C:\Windows\Fonts\segoeuib.ttf'      # Segoe UI Semibold
$FONT       = 'segoeuib.ttf'                       # referenced relative to _build/

# Encode settings. Beats are encoded identically so the final concat is a stream copy.
# The colour tags are not optional: video sources and PNG sources land on different
# defaults, and a mid-file colour-metadata change makes some players shift tone at the cut
# (and resets any filter graph reading the result).
$ENC = @('-r','30','-fps_mode','cfr','-pix_fmt','yuv420p',
         '-color_range','tv','-colorspace','bt709','-color_primaries','bt709','-color_trc','bt709',
         '-c:v','libx264','-crf','18','-preset','medium','-profile:v','high')

# ----------------------------------------------------------------------------------
# Helpers
# ----------------------------------------------------------------------------------

function Invoke-FF {
    param([Parameter(Mandatory)][string]$Label, [Parameter(Mandatory)][string[]]$FFArgs)
    Write-Host ("  {0}" -f $Label)
    & ffmpeg -hide_banner -loglevel error -y @FFArgs
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed while rendering: $Label" }
}

# Input args + shared encode profile + output name. Built in one place because PowerShell
# treats `+` as a literal argument, not an operator, at a call site.
function New-FFArgs {
    param([Parameter(Mandatory)][string[]]$In, [Parameter(Mandatory)][string]$Vf,
          [Parameter(Mandatory)][string]$Out)
    return $In + @('-vf', ($Vf + ',format=yuv420p')) + $ENC + @($Out)
}

# drawtext chokes on colons and quotes inside filter strings, so caption text is passed
# via textfile= instead of inline. UTF-8 without BOM or the first glyph renders as tofu.
function Write-TextFile {
    param([Parameter(Mandatory)][string]$Name, [Parameter(Mandatory)][string]$Text)
    $path = Join-Path $Build $Name
    [System.IO.File]::WriteAllText($path, $Text, (New-Object System.Text.UTF8Encoding($false)))
    return $Name
}

# Caption in the lower third, fading in at the start and out at the end of its beat.
function New-Caption {
    param([Parameter(Mandatory)][string]$File, [Parameter(Mandatory)][double]$Duration,
          [int]$Size = 46, [int]$FromBottom = 190)
    $d  = [math]::Round($Duration, 2)
    $a  = "if(lt(t,0.4),t/0.4,if(lt(t,$d-0.4),1,max(0,($d-t)/0.4)))"
    return "drawtext=fontfile=$($FONT):textfile=$($File):fontcolor=white:fontsize=$($Size)" +
           ":x=(w-text_w)/2:y=h-$($FromBottom):box=1:boxcolor=black@0.55:boxborderw=22" +
           ":alpha='$a'"
}

# Slow push-in on a still. Upscaling before zoompan is what keeps it from jittering.
function New-KenBurns {
    param([Parameter(Mandatory)][double]$Duration, [double]$MaxZoom = 1.10)
    $frames = [int]([math]::Round($Duration * 30))
    $step   = [math]::Round(($MaxZoom - 1.0) / $frames, 6)
    return "scale=-2:2160:flags=lanczos,crop=3840:2160," +
           "zoompan=z='min(zoom+$step,$MaxZoom)':d=1:fps=30:s=1920x1080" +
           ":x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
}

# ----------------------------------------------------------------------------------
# Preflight
# ----------------------------------------------------------------------------------

Write-Host 'Villager Chess trailer builder'

if (-not (Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
    throw 'ffmpeg is not on PATH. Install it or add C:\ffmpeg\bin to PATH.'
}
if (-not (Test-Path $FONT_FILE)) { throw "Font not found: $FONT_FILE" }

$sources = @{
    seg1   = Join-Path $Trailer 'seg1.mp4'
    seg2   = Join-Path $Trailer 'seg2.mp4'
    button = Join-Path $Shots '01-play-chess-button.png'
    check  = Join-Path $Shots '03-check.png'
    promo  = Join-Path $Shots '04-promotion-picker.png'
    win    = Join-Path $Shots '05-you-win.png'
    master = Join-Path $Shots '06-master-thinking.png'
}
foreach ($k in $sources.Keys) {
    if (-not (Test-Path $sources[$k])) { throw "Missing source asset: $($sources[$k])" }
}

if (Test-Path $Build) { Remove-Item $Build -Recurse -Force }
New-Item -ItemType Directory -Path $Build | Out-Null

# drawtext needs a font path without a drive-letter colon, so work from inside _build/
# with the font copied in beside the caption files.
Copy-Item $FONT_FILE (Join-Path $Build $FONT)

Push-Location $Build
try {
    $capApproach = Write-TextFile 'cap-approach.txt' $CAP_APPROACH
    $capTrade    = Write-TextFile 'cap-trade.txt'    $CAP_TRADE
    $capBoard    = Write-TextFile 'cap-board.txt'    $CAP_BOARD
    $capMoves    = Write-TextFile 'cap-moves.txt'    $CAP_MOVES
    $capCheck    = Write-TextFile 'cap-check.txt'    $CAP_CHECK
    $capPromo    = Write-TextFile 'cap-promo.txt'    $CAP_PROMO
    $capMaster   = Write-TextFile 'cap-master.txt'   $CAP_MASTER
    $capWin      = Write-TextFile 'cap-win.txt'      $CAP_WIN
    $txTitle     = Write-TextFile 'title-main.txt'   $TITLE_MAIN
    $txTitleSub  = Write-TextFile 'title-sub.txt'    $TITLE_SUB
    $txEndSub    = Write-TextFile 'end-sub.txt'      $END_SUB

    # ------------------------------------------------------------------------------
    # Beat 1 — title card
    # ------------------------------------------------------------------------------
    $vf = "drawtext=fontfile=$($FONT):textfile=$($txTitle):fontcolor=white:fontsize=104" +
          ":x=(w-text_w)/2:y=(h-text_h)/2-40," +
          "drawtext=fontfile=$($FONT):textfile=$($txTitleSub):fontcolor=0xC8C8C8:fontsize=44" +
          ":x=(w-text_w)/2:y=(h-text_h)/2+70," +
          "fade=t=in:st=0:d=0.5,fade=t=out:st=$($D_TITLE - 0.5):d=0.5"
    Invoke-FF 'beat 01  title card' (New-FFArgs `
        -In @('-f','lavfi','-t',"$D_TITLE",'-i','color=c=black:s=1920x1080:r=30') `
        -Vf $vf -Out 'beat01.mp4')

    # ------------------------------------------------------------------------------
    # Beat 2 — walking up to the villager (full frame)
    # ------------------------------------------------------------------------------
    $vf = "$FULL," + (New-Caption -File $capApproach -Duration $S1_APPROACH_DUR)
    Invoke-FF 'beat 02  walk-up' (New-FFArgs `
        -In @('-ss',"$S1_APPROACH_IN",'-t',"$S1_APPROACH_DUR",'-i',$sources.seg1) `
        -Vf $vf -Out 'beat02.mp4')

    # ------------------------------------------------------------------------------
    # Beat 3 — trade screen + Play Chess button (punch-in)
    # ------------------------------------------------------------------------------
    $vf = "$PUNCH," + (New-Caption -File $capTrade -Duration $S1_TRADE_DUR)
    Invoke-FF 'beat 03  trade screen' (New-FFArgs `
        -In @('-ss',"$S1_TRADE_IN",'-t',"$S1_TRADE_DUR",'-i',$sources.seg1) `
        -Vf $vf -Out 'beat03.mp4')

    # ------------------------------------------------------------------------------
    # Beat 4 — the board opens
    # ------------------------------------------------------------------------------
    $vf = "$PUNCH," + (New-Caption -File $capBoard -Duration $S1_BOARD_DUR)
    Invoke-FF 'beat 04  board opens' (New-FFArgs `
        -In @('-ss',"$S1_BOARD_IN",'-t',"$S1_BOARD_DUR",'-i',$sources.seg1) `
        -Vf $vf -Out 'beat04.mp4')

    # ------------------------------------------------------------------------------
    # Beat 5 — move montage, slightly sped up
    # ------------------------------------------------------------------------------
    $movesOut = [math]::Round($S2_MOVES_DUR * $S2_SPEED, 2)
    $vf = "$PUNCH,setpts=$($S2_SPEED)*PTS," + (New-Caption -File $capMoves -Duration $movesOut)
    Invoke-FF 'beat 05  move montage' (New-FFArgs `
        -In @('-ss',"$S2_MOVES_IN",'-t',"$S2_MOVES_DUR",'-i',$sources.seg2) `
        -Vf $vf -Out 'beat05.mp4')

    # ------------------------------------------------------------------------------
    # Beats 6-9 — feature stills with a slow push-in
    # ------------------------------------------------------------------------------
    $stills = @(
        @{ n='beat06'; label='beat 06  check';      img=$sources.check;  dur=$D_CHECK;  cap=$capCheck  },
        @{ n='beat07'; label='beat 07  promotion';  img=$sources.promo;  dur=$D_PROMO;  cap=$capPromo  },
        @{ n='beat08'; label='beat 08  the Master'; img=$sources.master; dur=$D_MASTER; cap=$capMaster },
        @{ n='beat09'; label='beat 09  you win';    img=$sources.win;    dur=$D_WIN;    cap=$capWin    }
    )
    # -framerate 30 matters: the image demuxer defaults to 25fps, and zoompan rewrites
    # timestamps from its own fps, which would render every still beat 1/6 short.
    foreach ($s in $stills) {
        $vf = (New-KenBurns -Duration $s.dur) + ',' + (New-Caption -File $s.cap -Duration $s.dur)
        Invoke-FF $s.label (New-FFArgs `
            -In @('-framerate','30','-loop','1','-t',"$($s.dur)",'-i',$s.img) `
            -Vf $vf -Out "$($s.n).mp4")
    }

    # ------------------------------------------------------------------------------
    # Beat 10 — end card over a darkened lead screenshot
    # ------------------------------------------------------------------------------
    $vf = "$FULL,drawbox=x=0:y=0:w=iw:h=ih:color=black@0.62:t=fill," +
          "drawtext=fontfile=$($FONT):textfile=$($txTitle):fontcolor=white:fontsize=96" +
          ":x=(w-text_w)/2:y=(h-text_h)/2-60," +
          "drawtext=fontfile=$($FONT):textfile=$($txTitleSub):fontcolor=0xC8C8C8:fontsize=42" +
          ":x=(w-text_w)/2:y=(h-text_h)/2+50," +
          "drawtext=fontfile=$($FONT):textfile=$($txEndSub):fontcolor=0x9FD08C:fontsize=38" +
          ":x=(w-text_w)/2:y=(h-text_h)/2+120," +
          "fade=t=in:st=0:d=0.4,fade=t=out:st=$($D_END - 0.8):d=0.8"
    Invoke-FF 'beat 10  end card' (New-FFArgs `
        -In @('-framerate','30','-loop','1','-t',"$D_END",'-i',$sources.button) `
        -Vf $vf -Out 'beat10.mp4')

    # ------------------------------------------------------------------------------
    # Concat + silent audio track
    # ------------------------------------------------------------------------------
    # Every beat shares one encode profile, so this is a stream copy: no generation loss
    # and no re-encode time. Stores and YouTube handle audio-less files inconsistently,
    # hence a silent AAC track rather than no audio at all.
    $beats = Get-ChildItem -Path $Build -Filter 'beat*.mp4' | Sort-Object Name
    $list  = ($beats | ForEach-Object { "file '$($_.Name)'" }) -join "`n"
    [System.IO.File]::WriteAllText((Join-Path $Build 'concat.txt'), $list,
        (New-Object System.Text.UTF8Encoding($false)))

    Invoke-FF 'final    concat + silent audio' @(
        '-f','concat','-safe','0','-i','concat.txt',
        '-f','lavfi','-i','anullsrc=r=48000:cl=stereo',
        '-map','0:v','-map','1:a','-c:v','copy','-c:a','aac','-b:a','128k',
        '-shortest','-movflags','+faststart',$OutFile)
}
finally {
    Pop-Location
}

$dur  = & ffprobe -v error -show_entries format=duration -of csv=p=0 $OutFile
$size = [math]::Round((Get-Item $OutFile).Length / 1MB, 1)
Write-Host ''
Write-Host ("Done: {0}" -f $OutFile)
Write-Host ("      {0}s, {1} MB" -f ([math]::Round([double]$dur, 1)), $size)
Write-Host '      Watch it, then adjust the TUNABLES block and re-run if anything is off.'
