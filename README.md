# OmegaT scripts

This repo contains Groovy scripts that can be run in OmegaT to automate or improve certain tasks.

* [Container assets management](#container-assets-management-container_assetsgroovy)
* [Copy source](#copy-source-copy_sourcegroovy)
* [Customization script](https://github.com/kosivantsov/omegat_scripts/tree/master/aux_scripts)
* [Transliteration]

## Container assets management [`container_assets.groovy`]

This script runs automatically when loading the project and downloads certain language assets (TM, glossary, style parameters, etc.) based on metadata included in the project settings or the project name, or in an external file (`containers_config.json`) included in a correctly customized OmegaT installation.

### Jargon

**container**: A "container" is the name of a project or client that has specific characteristics upon which certain language parameters depend (such as what style preferences, which reference TMs, etc. must be used). For example, `PISA` or `PIAAC` are containers.

### Pre-conditions

The following requirements must be met so that the script can work.

#### Remote pre-conditions
1. The language assets to be downloaded (TMX files, TSV glossaries, etc.) must be:
	* stored in a folder in a server
	* valid TMX files (according to the TMX 1.1 specification) or valid TSV glossary files according to OmegaT's format
2. A `hash_list.txt` file must be available in that same folder. The `hash_list.txt` file must:
	* contain one line per file available in that folder, including the md5 hash value for each file and the file name, separated by a colon, e.g.`3c28899d88ab74a81075513d2a1f63fd:ESS_Glossary_afr-ZAF.utf8`;
	* be created with the PHP script `name` (or by any other means as long as the hash values are the same as the ones generated by that PHP script);
	* be updated every time new language asset files are added or updated in that directory (in a Linux server, there is a Linux utility called [incron](http://manpages.ubuntu.com/manpages/focal/en/man5/incrontab.5.html) that can be used similar to normal cron, but rather than events being time based, it works off of inotify and is fired from file events (e.g. `/var/www/path/to/watched/folder IN_CLOSE_WRITE $(which php) -f hash_list.php`).

#### Local pre-conditions
1. The OmegaT installation must have been customized and the customization must be up to date.
2. The URLs to the remote directories containing language assets to be downloaded must be included in a properties file (`containers_config.json`) saved in the configuration folder.
3. The name of the OmegaT project must observe a number of requirements:
	* It starts with the name of the container (case-sensitive)
	* In 2021: it includes the three-letter (cApStAn or ETS) target-language code (e.g. `glg-ESP` for Galician)

### Execution

The script runs automatically when the user opens any project.

1. Firstly, it fetches some properties from the project settings (language pair) or the project name (container)
2. Secondly, it checks in the global container assets config file whether the container is listed there. (@TODO)
    - If it is not, it stops there and does not download any file.
3. If the container is recognized, it tries to download the language assets available and puts them in the right place: TMX files go to `/tm/ref`, glossaries go to `/glossary`, etc.
    - To avoid re-downloading every time files that were already downloaded, the `hash_list.txt` file is used to determine whether each available asset is new (now downloaded before) or has been updated.

## TODO

### Security

Add some HTTP Basic Authentication (a `.htaccess` file) on the server and include the login/password in the config file.

### Config file

Java properties should be easy to edit for anyone while not too flat to store container's properties.

```
# Test
test.domain=http://...

# Container
container.property1=value
container.property2=another value
```

### ETag

Using ETag header makes the hash list unnecessary, so it gets simpler for the server sysadmin.

### Recursivity

In case the server sysadmin organizes assets in folders and subfolders by container and language code, the script should parse the remote directory recursively.

### Delete old assets?

TBC: Add a function to check which local assets are not found anymore in any repo.

## Copy source [`copy_source.groovy`]
