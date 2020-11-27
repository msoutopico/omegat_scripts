/* :name = Convert Arabic Digits in Project (Western to Eastern) :description=Transliterates any Western digit, if found, to its Arabic-Indic counterpart
 * 
 * @author      Manuel Souto Pico
 * @date        2019-05-07
 * @version     0.0.2
 */

def gui(){
	def segm_count = 0
	def pattern;
	def replac;
	def numbers;
	def search;

	//pattern = /[0-9]/
	pattern = /[0-9](?![^<]*?>)/

	numbers = ["٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"]

	project.allEntries.each { ste ->

		def target = project.getTranslationInfo(ste) ? project.getTranslationInfo(ste).translation : null;
		def repl;
		repl = target;

	 	if (target != null) 
	 	{
	 		if (target.find(pattern) != null) 
	 		{
	 					
			segm_count++
			editor.gotoEntry(ste.entryNum())
			numbers.eachWithIndex { raqm, index ->
    			console.println "${index} => ${raqm}"
				search = /${index}(?![^<]*?>)/
				console.println("Find ${search} and replace it with ${raqm}")
				replac = "${raqm}"

				//def repl = target.tr('0123456789', '٠١٢٣٤٥٦٧٨٩')
				repl = repl.replaceAll(search, replac)
			}

			console.println(ste.entryNum() + "\t" + target + " => " + repl)
			editor.replaceEditText(repl)
			}
	 	} 
	 	
	}
	console.println(segm_count + " segments modified")
}