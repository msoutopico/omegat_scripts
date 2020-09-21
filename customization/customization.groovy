/* :name = Customization :description =Download langauge assets for a specific container and language pair
 *
 *  @version: 0.1.0
 *  @author: Manuel Souto Pico
 */

 /* Changes:
  *
  *  0.1.0: added hash comparison for all config files
  */



// first check: is the project being opened?
import org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE
title = "Customization"


// my imports
import org.apache.commons.io.FilenameUtils
import groovy.io.FileType
//

// modules
import groovy.util.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.apache.commons.io.FileUtils
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.omegat.util.StaticUtils
import java.security.MessageDigest
import static groovy.io.FileType.FILES


import org.omegat.core.events.IApplicationEventListener

import groovy.swing.SwingBuilder
import groovy.util.XmlSlurper
//import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.FlowLayout
import java.awt.GridBagConstraints as GBC
import java.awt.GridBagLayout as GBL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JProgressBar
import javax.swing.WindowConstants
import org.apache.commons.io.FileUtils
import org.omegat.CLIParameters
import org.omegat.util.Preferences
import org.omegat.util.StaticUtils
import org.omegat.util.StringUtil
import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

// unused
utils = (StringUtil.getMethods().toString().findAll("makeValidXML")) ? StringUtil : StaticUtils

String.metaClass.confirm = { ->
    showConfirmDialog null, delegate, title, YES_NO_OPTION
}

String.metaClass.alert = { ->
    showMessageDialog null, delegate, title, INFORMATION_MESSAGE
}


// the script starts here if a project is open
console.println("="*40 + "\n" + " "*5 + "Customization\n" + "="*40)

// constants
digest = MessageDigest.getInstance("MD5") // not using def gives the variable global scope
prop = project.getProjectProperties()

// The container is the first entity in the project name (before the first underscore)




timestamp = new Date().format("YYYYMMddHHmm")
config_dir = StaticUtils.getConfigDir()
config_file = new File(config_dir + "containers_config.properties")

// def tgt_code = 			project.projectProperties.targetLanguage
// def src_code = 			project.projectProperties.sourceLanguage
// def tgt_lang_subtag = 	project.projectProperties.targetLanguage.languageCode
// def src_lang_subtag = 	project.projectProperties.sourceLanguage.languageCode
// def tgt_region_subtag = project.projectProperties.targetLanguage.countryCode
// def src_region_subtag = project.projectProperties.sourceLanguage.countryCode

// https://www.baeldung.com/groovy-file-read
String read_file_string(String path_to_file) {
    File file = new File(path_to_file)
    String file_content = file.text
    return file_content
}

def download_asset(urlstr_to_remote_asset, dest_dir) {

	URL url = new URL(urlstr_to_remote_asset)
	remote_asset_name = FilenameUtils.getName(url.getPath()) // -> file.xml
	// console.println(FilenameUtils.getBaseName(url.getPath())); // -> file
	// console.println(FilenameUtils.getExtension(url.getPath())); // -> xml
	console.println("--\nDOWNLOAD ${remote_asset_name} from ${urlstr_to_remote_asset}.")

	try {
		// def url = domain + "files/" + remote_asset_name
		def dest_file = new File(dest_dir.toString() + File.separator + remote_asset_name)
		dest_file.withOutputStream { output_stream  ->

			// if url exists??? jarx

			// def url = new URL(urlstr_to_remote_asset).openConnection()
			def conn = url.openConnection()
			output_stream << conn.inputStream
			// FileUtils.copyInputStreamToFile(url.toURL().openStream(), dest_file)
		}
	} catch (IOException e) {
		// unable to download asset
		console.println("Unable to download asset: " + e.message)
		return
	}
}







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
		console.println("Unable to download hash list: " + e.message)
		return // stop script if list of hashes not available?? or just download everything found?
	}
}

def update_assets(domain, dest) {

	def local_config_dir = new File(dest)
	def remote_config_dir = domain + "config/"

	url_to_hash_list = domain + hash_filename
	hash_list = fetch_hash_list(url_to_hash_list)

	if (!hash_list) {
		console.println("No hash list found, unable to continue.")
		return
	}
	hash_list.each { it ->
		// console.println("line in hash list: " + it)
	}

	// put remote list of hashes in array (filename -> hash)
	//
	// Pattern re = ~/${container}_(?:.*?)_Glossary_${tgt_code}/
	// def match = re.matcher(str).asBoolean()

	// moveing the list to a map
	def remote_file_hash_map = hash_list.collectEntries {
		def hash = it.split(':')[0]
		def file = it.split(':')[1]
		[(file): hash]
	}
	remote_file_hash_map.each {
		// console.println("line in hash map: " + it.key + ":" + it.value)
	}
	// get local assets and their hashes
	local_file_hash_map = get_local_hash_list(dest, remote_file_hash_map.keySet())


	local_file_hash_map.each {
		// console.println("line in local hash map: " + it.key + ":" + it.value)
	}

	// compare remote to local and get updated assets
	remote_file_hash_map.each {
		remote_asset_name = it.key
		remote_asset_hash = it.value


		def message
		// if the remote asset is found in the assets folder
		def downloaded = local_file_hash_map.containsKey(remote_asset_name)
		if (downloaded) {
			def local_asset_hash = local_file_hash_map.find{ it.key == remote_asset_name }?.value
			if (local_asset_hash) {
				if (local_asset_hash == remote_asset_hash) {
					// message = "Remote asset ${remote_asset_name} hasn't changed, the local copy is up to date."
					message = "..."
				} else {
					message = "Remote asset ${remote_asset_name} has been updated, a new download is needed."
					// download
					download_asset(remote_config_dir + remote_asset_name, local_config_dir)
				}
			}
		} else {
			// download
			// check if it exists locally, if it does, then msg:
			message = "Remote asset ${remote_asset_name} is new or had not been downloaded, will be downloaded now."
			// download
			download_asset(remote_config_dir + remote_asset_name, local_config_dir)
		}

		console.println(message)
	}
	console.println("Done!\n")
}

def delete_other_versions(urlstr_to_remote_asset, dest_dir) {

	/*
		this fucntion should delete the following jar files from directory <config_dir>\plugins
		* okapiFiltersForOmegaT-1.6-m40-custom.jar
		* okapiFiltersForOmegaT-1.7-1.40.0-SNAPSHOT.jar
		* plugin-omt-package-1.4.1.jar
		* plugin-omt-package-1.6.1.jar
		(sample files for testing available here: https://capps.capstan.be/Files/plugins.zip)

		The download_asset function should download (and overwrite):
		* omegat-bidimarkers-0.2.0-all.jar
		* okapiFiltersForOmegaT-1.6-m40-capstan.jar
		* plugin-omt-package-1.6.3.jar
		from https://cat.capstan.be/OmegaT/installer/plugins/
	*/

	URL url = new URL(urlstr_to_remote_asset)
	remote_asset_name = FilenameUtils.getName(url.getPath()) // -> file.xml

	def lib_name = remote_asset_name.minus(~/-\d+.*\.jar$/)
	lib_to_delete = lib_name + "*.jar"
	// console.println("\nI will delete " + lib_to_delete)
	def files_to_delete = new FileNameFinder().getFileNames(dest_dir, lib_name + '*.jar' /* includes */, remote_asset_name /* excludes */)

	files_to_delete.each { it -> // it is a path

		console.println("***\nDELETE " + it)
		def file = new File(it)

		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(0);
			FileOutputStream fos = new FileOutputStream(file);
			// this will deplete the file, so it can be deleted afterwards by cmd
			bos.writeTo(fos);
			fos.close();
			// System.gc()
			// file.deleteOnExit() // me beating about the bush
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//
	// String folderPath = dest_dir
	// new File(folderPath).eachFile (FileType.FILES) { file ->
	// 		//Delete file if file name contains Jenkins
	//    if (file.name.contains(lib_name)) file.delete()
	// }

}

files = [ "omegat-bidimarkers-0.2.0-all.jar", "plugin-omt-package-1.6.3.jar", "okapiFiltersForOmegaT-1.6-m40-capstan.jar" ]
// "okapiFiltersForOmegaT-1.6-m40-capstan.jar",


//
dest_dir = config_dir + "plugins"
domain = "https://cat.capstan.be/OmegaT/custom/"
// def list_url = 			tb_domain + "list_contents.php"
hash_filename = "hash_list.txt"
update_assets(domain, config_dir)

files.each { file ->
	url_str = domain + "plugins/" + file
	// delete_other_versions(url_str, dest_dir)
	// download_asset(url_str, dest_dir)
}


// console.println("eventType: " + eventType)
// console.println("Collecting garbage...")
System.gc()
// console.println("All garbage collected. Ready for next run.")
return
