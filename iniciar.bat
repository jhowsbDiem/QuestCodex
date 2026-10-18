@echo off
chcp 65001 >nul
title Servidor Minecraft Paper 26.3 - QuestCodex

REM 1. Se o paper-26.3-205.jar nao existir (ex: novo clone), baixa automaticamente da API do PaperMC
if not exist "paper-26.3-205.jar" (
    echo [QuestCodex] Baixando Paper 26.3-205...
    powershell -NoProfile -Command "$ua='QuestCodex/1.0'; $url=(Invoke-RestMethod -Uri 'https://fill.papermc.io/v3/projects/paper/versions/26.3/builds/latest' -Headers @{'User-Agent'=$ua}).downloads.'server:default'.url; Invoke-WebRequest -Uri $url -Headers @{'User-Agent'=$ua} -OutFile 'paper-26.3-205.jar'"
)

REM 2. Garante eula.txt e server.properties com a Seed oficial (7311929229522424735) e Dificuldade HARD
if not exist "eula.txt" (
    echo eula=true> eula.txt
)
if not exist "server.properties" (
    (
        echo level-name=world
        echo level-seed=7311929229522424735
        echo gamemode=survival
        echo difficulty=hard
        echo spawn-protection=0
        echo white-list=false
        echo online-mode=true
        echo simulation-distance=10
        echo view-distance=10
        echo motd=\u00A76\u00A7lQUEST CODEX \u00A78- \u00A7eSurvival Competitivo por Conquistas
    ) > server.properties
)

REM 3. Se a partida anterior encerrou e marcou reset-world.flag, apaga o mapa para gerar um novo limpo!
if exist "reset-world.flag" (
    echo [QuestCodex] Resetando o mapa ^(world^) apos o fim da partida anterior...
    if exist "world" rmdir /s /q "world"
    if exist "world_nether" rmdir /s /q "world_nether"
    if exist "world_the_end" rmdir /s /q "world_the_end"
    del /f /q "reset-world.flag"
    echo [QuestCodex] Mapa resetado com sucesso! Um novo mapa sera gerado na seed oficial.
)

REM 4. Compila os plugins automaticamente se a biblioteca do Paper ja estiver extraida
if exist "libraries\io\papermc\paper\paper-api" (
    echo [QuestCodex] Compilando plugins atualizados ^(PingPlugin e TagsPlugin^)...
    powershell -NoProfile -ExecutionPolicy Bypass -File "compilar-plugins.ps1"
)

java -Xms6G -Xmx6G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -jar paper-26.3-205.jar --nogui
pause