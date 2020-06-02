# OmegaT scripts

This repo contains Groovy scripts that can be run in OmegaT to automate or improve certain tasks.

## container_assets.groovy

This script runs automatically when loading the project.

The script downloads certain language assets (TM, glossary, style parameters, etc.) based on metadata included in the project settings or project name.

This are the steps:
* It fetches some properties from the project settings (language pair) or the project name (container)
* It checks in file `container_assets.properties` whether the container is listed there.
  - If it is not, it stops there
  - it is, it tries to download assets available and puts them in the right place:
    - TMX files go to `/tm/capps`
	- glossaries go to `/glossary`
