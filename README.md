# OmegaT scripts

This repo contains Groovy scripts that can be run in OmegaT to automate or improve certain tasks.

## container_assets.groovy

This script runs automatically when loading the project and downloads certain language assets (TM, glossary, style parameters, etc.) based on metadata included in the project settings or project name.

1. Firstly, it fetches some properties from the project settings (language pair) or the project name (container)
2. Secondly, it checks in file `container_assets.json` whether the container is listed there.
- If it is not, it stops there.
- it is, it tries to download assets available and puts them in the right place: TMX files go to `/tm/capps`, glossaries go to `/glossary`, etc.

### How to prepare language assets for your container

url
json file

## copy_source.groovy
