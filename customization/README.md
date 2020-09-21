# Documentation

## Customization script [`customization.groovy`]

### Customization

OmegaT can be customized by means of:

* plugins (and their properties files)
* scripts (and their properties files)
* other configuration files

All these custom files will be saved in the **user configuration folder**. Plugins and scripts are saved in their respective subfolders, `plugins` and `scripts`, and the rest of the configuration files are written directly to the user configuration folder.

<!-- ### Options

| Option | Description |
|:-------|:------------|
| `customUrl` | insert URL between quotes or set to "" (empty) to ask the user on the first run. |
| `autoLaunch` | If set to `true` and the script is saved at `scrips\application_startup`, the script will run when OmegaT is started. |
| `removeExtraPlugins` | Lets you delete any loose jar files in the `plugins` folder under the **installation folder**. |
| `deletePlugVerbose` | If set to true, makes the script list the jar files to be removed manually. If set to false, it makes the script remind the user to remove plugins from the `plugins` folder under the **installation folder**. | -->

### Remote requirements

The script will fetch custom files from a remote location that recreates the structure of the user configuration folder:

	config/
	|_ plugins/
	|_ scripts/
	|_ omegat.autotext
	|_ omegat.prefs
	|_ uiLayout.xml
	hash_list.txt

At the same level of the remote `config` folder the script expects to find a text file called `hash_list.txt` by default (customizable at the top of the script).

This file will contain one line for each custom file to be added to the user's installation, including the MD5 hash value of the file, followed by a colon and the relative path to the file under the config folder.

For example:

	87a334c47480b81c060cd496d136dc63:omegat.autotext
	7848793a66e7e294510466d5279a6157:omegat.prefs
	296eaa1703b2e65f3a6b7242e32a66df:omt-package-config.properties
	1a3a9150c41cdc9b2b02774b53f2370e:plugins/okapiFiltersForOmegaT-1.6-m40-capstan.jar
	3feef6d111433ee69d9be35881376909:plugins/plugin-omt-package-1.6.3.jar
	378e7e60db0adb116550bebb492dda4c:scripts/convert_arab2indic_global.groovy
	766e949781ba00502e920b45c6e20ef5:scripts/convert_indic2arab_global.groovy
	a05b885118d4e3a67752ff2e90d029f5:scripts/copy_source.groovy
	67ae6b05e682f01fa1708e46fd2c241c:scripts/flush_working_tm.groovy
	5dbbe0818d6547582f95eacce56f2a04:scripts/merge_2lang.groovy
	248f3109183eef38d474c55583874f96:scripts/project_changed/container_assets.groovy
	2dd1d7e0ec732c271b404d00c9b50115:scripts/project_changed/pisaconv.groovy
	eca337219963c9e745fac029696c0a07:scripts/project_changed/restore_files_order.groovy
	2045709e1dcc248ce753803de43695f8:scripts/pseudoxlate.groovy
	4823702bcbb7ec847d3db47aef9b2da7:uiLayout.xml

The server administrator is responsible for keeping this file up to date whenever a file is updated in the remote location.

### Business logic

In order for the script to run, the user must provide the URL to the remote location that contains the `config` folder and the `hash_file.txt` file, e.g. `https://cat.capstan.be/OmegaT/custom/`. Without the URL to the remote location where a valid `hash_file.txt` can be fetched, the script will be interrupted.

The script will fetch the list of custom files together with their hash value and will look for them in the user configuration folder. If a file is not found there, it will be downloaded.

For files other than plugins, if the file is found locally, then the script will compare the local and the remote versions of the file to see they are identical or different. If they are different, the remote version will be downloaded. This comparison is made based on the hash values of the files.

Every time the custom files are updated in the remote location, the line with the hash value and the filename must be added or updated in the `hash_file.txt` file.

### Caveats

#### Plugins

The script will also try to delete plugins used in the customization from the `plugins` folder in the **installation folder** (or prompt the user to delete them manually) to avoid conflicts with the plugins written in the user configuration folder.

To avoid conflicts, the script also tries to delete all jar files from the **installation folder** (if that folder is writeable) or prompts the user do so manually (if the folder is read-only).

Any jar files that the user might wish to keep can be saved under a subfolder in the `plugins` folder, e.g. `plugins\my_folder`. That way they will be protected and the customization script will not touch them.

#### Restart

To make sure that the new settings are applied (in case there is more than instance running), OmegaT will close after the customization update. It needs to be restarted manually. Since the check for updates happens when OmegaT is launched and before any project is open, if there is any update available, it'll have to be started twice.

### Locations
#### Where is the user configuration folder

The user can reach that folder in two ways:

* From OmegaT, go to **Tool > User Configuration Folder**
* From the operating system, (in Windows) press **Win+R** and type `%appdata%/OmegaT`.

#### Where is the installation folder

On a 64-bit machine under Windows 10, it is by default on path `C:\Program Files\OmegaT`.
