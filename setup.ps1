Write-Host "=== Metamodel Coevolution - Setup ===" -ForegroundColor Cyan

# 1. Python dependencies
Write-Host "`n[1/3] Installation des dependances Python..." -ForegroundColor Yellow
Set-Location python
pip install -r requirements.txt
Set-Location ..

# 2. Build Java
Write-Host "`n[2/3] Build Java (collector + migrator)..." -ForegroundColor Yellow
Set-Location java\collector
mvn clean install -q
Set-Location ..\..\java\migrator
mvn clean install -q
Set-Location ..\..

# 3. Launch GUI
Write-Host "`n[3/3] Lancement de la GUI JavaFX..." -ForegroundColor Green
Set-Location java\gui
mvn clean javafx:run

Write-Host "`n=== FIN ===" -ForegroundColor Cyan