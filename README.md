cd C:\Users\maya mohammed\ecli\model-metamodel-coevolution5\java\collector

$repos_pairs    = (Get-ChildItem "dataset_repos\pairs"   -Directory).Count
$domains_pairs  = (Get-ChildItem "dataset_domains\pairs" -Directory).Count
$synth_pairs    = (Get-ChildItem "dataset_synth\pairs"   -Directory -ErrorAction SilentlyContinue).Count

$repos_aug      = (Get-ChildItem "..\augmentation\augmented_repos"   -Directory -ErrorAction SilentlyContinue).Count
$domains_aug    = (Get-ChildItem "..\augmentation\augmented_domains" -Directory -ErrorAction SilentlyContinue).Count
$synth_aug      = (Get-ChildItem "..\augmentation\augmented_synth"   -Directory -ErrorAction SilentlyContinue).Count

$total_orig     = $repos_pairs + $domains_pairs + $synth_pairs
$total_aug      = $repos_aug + $domains_aug + $synth_aug
$total          = $total_orig + $total_aug

$size_repos     = [math]::Round((Get-ChildItem "dataset_repos\pairs"   -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
$size_domains   = [math]::Round((Get-ChildItem "dataset_domains\pairs" -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB, 2)

Write-Host "==================================================="
Write-Host "  DATASET ORIGINAL"
Write-Host "---------------------------------------------------"
Write-Host "  Repos   pairs : $repos_pairs"
Write-Host "  Domains pairs : $domains_pairs"
Write-Host "  Synth   pairs : $synth_pairs"
Write-Host "  Sous-total    : $total_orig"
Write-Host "==================================================="
Write-Host "  DATASET AUGMENTE"
Write-Host "---------------------------------------------------"
Write-Host "  Repos   aug   : $repos_aug"
Write-Host "  Domains aug   : $domains_aug"
Write-Host "  Synth   aug   : $synth_aug"
Write-Host "  Sous-total    : $total_aug"
Write-Host "==================================================="
Write-Host "  TAILLES"
Write-Host "---------------------------------------------------"
Write-Host "  dataset_repos   : $size_repos MB"
Write-Host "  dataset_domains : $size_domains MB"
Write-Host "==================================================="
Write-Host "  TOTAL DATASET   : $total PAIRES"
Write-Host "==================================================="

