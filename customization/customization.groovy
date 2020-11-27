/* :name = Customization :description =Download langauge assets for a specific container and language pair
 *
 *  @version: 0.4.0
 *  @author: Manuel Souto Pico
 */





// user-defined parameter
domain = ""
hash_filename = "hash_list.txt"
if (!domain) {
	console.println("No URL for the remote location defined.")
	return
}


 /* Changes:
  *
  *  0.1.0: added hash comparison for all config files
  *  0.2.0: remote and local plugins are compared based on filename
  *  0.3.0: remote and local scripts and config files based on hash
  *  0.4.0: domain entered by user or read from properties
  *  0.5.0: copy scripts from install_dir/scripts
  *  0.6.0: edit
  *  0.6.0: delete plugins from install_dir/scripts
  *  0.6.0: add prompt to user to delete old plugins manually (for Windows)
  *  0.7.0: get %APPDATA% for execution by OmegaT.
  *  0.8.0: prompt the user to restart OmegaT to use the latest files
  *  0.9.0: checks the user ID and installs additional stuff if matches pattern (e.g. startswith("VER_"))
  */

// first check: is the project being opened?
// import org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE
title = "Customization"

// my imports
import org.apache.commons.io.FilenameUtils
import groovy.io.FileType
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import org.omegat.util.StaticUtils
import static groovy.io.FileType.FILES

// // modules
// import groovy.util.*
// import java.io.File
// import java.nio.file.Files
// import org.apache.commons.io.FileUtils
// import java.io.IOException
// import java.util.regex.Matcher
// import java.util.regex.Pattern
// import org.omegat.core.events.IApplicationEventListener
// import groovy.swing.SwingBuilder
// import groovy.util.XmlSlurper
// //import java.awt.*
// import java.awt.event.WindowAdapter
// import java.awt.event.WindowEvent
// import java.awt.FlowLayout
// import java.awt.GridBagConstraints as GBC
// import java.awt.GridBagLayout as GBL
// import java.nio.file.Files
// import java.nio.file.Paths
// import java.util.zip.ZipFile
// import javax.swing.JLabel
// import javax.swing.JOptionPane
// import javax.swing.JProgressBar
// import javax.swing.WindowConstants
// import org.apache.commons.io.FileUtils
// import org.omegat.CLIParameters
// import org.omegat.util.Preferences
// import org.omegat.util.StaticUtils
// import org.omegat.util.StringUtil
// import static javax.swing.JOptionPane.*
// import static org.omegat.util.Platform.*

// the script starts here if a project is open
console.println("="*40 + "\n" + " "*5 + "Customization\n" + "="*40)

// def tgt_code = 			project.projectProperties.targetLanguage
// def src_code = 			project.projectProperties.sourceLanguage
// def tgt_lang_subtag = 	project.projectProperties.targetLanguage.languageCode
// def src_lang_subtag = 	project.projectProperties.sourceLanguage.languageCode
// def tgt_region_subtag = project.projectProperties.targetLanguage.countryCode
// def src_region_subtag = project.projectProperties.sourceLanguage.countryCode

def get_local_hash_list(dest, remote_file_list) {

	// assert dest == config_dir
	def local_config_dir = new File(dest) // either glossary_dir or tm_dir
	def local_file_hash_map = [:]

	local_config_dir.traverse(type: FILES, maxDepth: 2) { it ->
		// @todo: should this be done asynchronously perhaps?

		// create object of Path
		Path abs_path = Paths.get(it.path)

		def win_rel_path = local_config_dir.toPath().relativize( it.toPath() ).toFile()
		def unix_rel_path = FilenameUtils.separatorsToUnix(win_rel_path.toString())

		// String asset_name = abs_path.getFileName()
		// call getFileName() and get FileName as a string

		if (remote_file_list.contains(unix_rel_path)) {
			// https://128bit.io/2011/02/17/md5-hashing-in-python-ruby-and-groovy/
			def asset_hash = new BigInteger(1,digest.digest(it.getBytes())).toString(16).padLeft(32,"0")
			// console.println(asset_hash + " <= " + unix_rel_path)
			local_file_hash_map.put(unix_rel_path, asset_hash)
		}
	}
	// String newString = str.replace("\\","/");
	return local_file_hash_map
}

def fetch_hash_list(url_to_hash_list) {

	try {
		// def local_path = new File(config_dir.toString() + File.separator + "asset_hashlist.txt")
		def url = new URL(url_to_hash_list).openConnection()
		return url.inputStream.readLines()
	}  catch (IOException e) {
		// last_modif will stay an empty array
		message.add("!! Unable to download hash list: " + e.message)
		return // stop script if list of hashes not available?? or just download everything found?
	}
}

def download_asset(urlstr_to_remote_file) {

	URL url = new URL(urlstr_to_remote_file)
	def remote_file_name = FilenameUtils.getName(url.getPath()) // -> file.xml
	def rel_path_str_to_dest = urlstr_to_remote_file.minus(domain + "config/").toString()

	def dest = config_dir + rel_path_str_to_dest
	def local_path_to_dest = ( System.properties['os.name'].toLowerCase().contains('windows') ? FilenameUtils.separatorsToWindows(dest) : dest )

	message.add(">> DOWNLOAD " + remote_file_name + " to " + rel_path_str_to_dest)

	try {
		// def url = domain + "files/" + remote_file_name
		def dest_file = new File(local_path_to_dest.toString())
		dest_file.withOutputStream { output_stream  ->

			def conn = url.openConnection()
			output_stream << conn.inputStream
		}
	} catch (IOException e) {
		message.add("!! Unable to download file: " + e.message)
		return
	}
}

def delete_old_plugins(new_jar_relpath, plugins_dir) {

	// URL url = new URL(urlstr_to_remote_file)
	new_jar_name = FilenameUtils.getName(new_jar_relpath)
	jar_files = new_jar_name.minus(~/-\d+.*\.jar$/) + "*.jar"
	// console.println("\nI will delete files with pattern " + jar_files)
	def files_to_delete = new FileNameFinder().getFileNames(plugins_dir.toString(), jar_files /* includes */, new_jar_name /* excludes */)

	files_to_delete.each { it -> // it is a path
		def file = new File(it)
		file.delete() // on Windows this might not work if OmegaT is running>
		message.add("--- DELETE (at least try) $new_jar_name")
		// // some black magic to byass window's lock?
	}
}

def fetch_plugins_by_name(local_file_hash_map, remote_plugins) {
	// fetch plugins' jar files (comparing remote to local filenames)
	remote_plugins.each { plugin ->
		// console.println(it)
		def downloaded = local_file_hash_map.containsKey(plugin)
		if (!downloaded) {
			delete_old_plugins(plugin, plugins_dir)
			download_asset(remote_config_dir + plugin)
			message.add("++ Remote custom file " + plugin + " downloaded to " + plugins_dir)
		} else {
			// strange output if this line is not commented...
			// message.add("== Remote custom file " + plugin + " is already up to date")
		}
	}
}

def fetch_files_by_hash(local_file_hash_map, remote_file_hash_map) {
	// get other custom files (compare remote to local hashes)
	remote_file_hash_map.each {
		remote_file_name = it.key
		remote_file_hash = it.value

		// if the remote asset is found in the assets folder
		def downloaded = local_file_hash_map.containsKey(remote_file_name)
		if (downloaded) {
			def local_asset_hash = local_file_hash_map.find{ it.key == remote_file_name }?.value
			if (local_asset_hash) {
				if (local_asset_hash == remote_file_hash) {
					// message.add("== Remote custom file ${remote_file_name} hasn't changed, the local copy is up to date.")
				} else {
					message.add("++ Remote custom file " + remote_file_name + " has been updated, downloading now.")
					// download
					download_asset(remote_config_dir + remote_file_name)
				}
			}
		} else {
			// download
			// check if it exists locally, if it does, then msg:
			message.add("++ Remote custom file " + remote_file_name + " is new or had not been downloaded, downloading now.")
			// download
			download_asset(remote_config_dir + remote_file_name)
		}
	}
}

def update_assets() {

	// def local_config_dir = new File(dest)
	def url_to_hash_list = domain + hash_filename
	def hash_list = fetch_hash_list(url_to_hash_list)

	if (!hash_list) {
		message.add("!!! No hash list found, unable to continue.")
		return
	}

	// moveing the list to a map
	def remote_file_hash_map = hash_list.collectEntries {
		def hash = it.split(':')[0]
		def file = it.split(':')[1]
		[(file): hash]
	}

	// get local assets and their hashes
	def local_file_hash_map = get_local_hash_list(config_dir, remote_file_hash_map.keySet())

	// get list of plugins
	def remote_plugins = (remote_file_hash_map.findAll { it.key.startsWith("plugins/") }).keySet()
	fetch_plugins_by_name( local_file_hash_map, remote_plugins)
	// get map of the rest of custom files (config and scripts)
	remote_file_hash_map = remote_file_hash_map.findAll { !it.key.startsWith("plugins/") }
	fetch_files_by_hash(   local_file_hash_map, remote_file_hash_map)

}

message = []
assert message.empty
// constants
digest = MessageDigest.getInstance("MD5") // not using def gives the variable global scope
// timestamp = new Date().format("YYYYMMddHHmm")

// if omegat internals are available
config_dir = StaticUtils.getConfigDir()
// otherwise:
// if OmegaT is not running
omegat_appdata = System.getenv("APPDATA") + File.separator + "OmegaT"
appdata = ( System.properties['os.name'].toLowerCase().contains('windows') ?  omegat_appdata : "~/.omegat" )
// e.g. C:\Users\souto\AppData\Roaming\OmegaT

plugins_dir = new File(config_dir.toString() + File.separator + "plugins")
scripts_dir = new File(config_dir.toString() + File.separator + "scripts")
// dest_dir = config_dir + "plugins"
// def list_url = 			tb_domain + "list_contents.php"
remote_config_dir = domain + "config/"
// main function
update_assets()

no_updates_msg = "All custom files were already up to date, nothing downloaded."
// message.each { line -> console.println(line) }
console.println( !message.empty ? message.join('\n') : no_updates_msg )
console.println("Done!")

// console.println("eventType: " + eventType)
// console.println("Collecting garbage...")
System.gc()
// console.println("All garbage collected. Ready for next run.")
return
