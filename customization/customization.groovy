/* :name = Customization :description =Download langauge assets for a specific container and language pair
 *
 *  @version: 0.1.0
 *  @author: Manuel Souto Pico
 */

/*
 * Changes per version:
 * 0.1.0 	Finalized plugin download
 * 0.2.0	Finalized script download
 * 0.3.0	Finalized config files download
 * 0.4.0	Master list of custom files
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
domain = "https://cat.capstan.be/OmegaT/installer/"

files.each { file ->
	url_str = domain + "plugins/" + file
	delete_other_versions(url_str, dest_dir)
	download_asset(url_str, dest_dir)
}


// console.println("eventType: " + eventType)
// console.println("Collecting garbage...")
System.gc()
// console.println("All garbage collected. Ready for next run.")
return
