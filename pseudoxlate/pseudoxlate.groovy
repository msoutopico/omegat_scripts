/* :name = Pseudo-translate project :description=
 * 
 * @author      Manuel Souto Pico
 * @date        2020-09-12
 * @version     0.2.0
 */

/* 
 * @versions: 
 * 0.2.0: added %CODE% exclusion
 */
 
def gui(){
	def segm_count = 0;

	project.allEntries.each { currSegm ->
		editor.gotoEntry(currSegm.entryNum())
		def target = currSegm.getSrcText();

		// pseudo-translate upper case letters 
		search = /\p{Lu}(?![^<]*?>|[^%\s]*?%|[^{]*?\})/   
		replac = "X"
		target = target.replaceAll(search, replac)

		// pseudo-translate lower case letters 
		search = /\p{Ll}(?![^<]*?>|[^%\s]*?%|[^{]*?\})/   
		replac = "x"
		target = target.replaceAll(search, replac)

		// pseudo-translate ditis (in any case ;)
		search = /\p{N}(?![^<]*?>|[^%\s]*?%|[^{]*?\})/   
		replac = "#"
		target = target.replaceAll(search, replac)
				
		segm_count++;
		console.println(currSegm.entryNum() + ": '" + currSegm.getSrcText() + "' pseudo-translated as '" + target + "'")
		editor.replaceEditText(target)
	}
	console.println(segm_count + " segments modified")
}

/*
def gui() {
	project.allEntries.each { ste ->
	
		editor.gotoEntry(ste.entryNum())
		// copy source 
		def pseudo = ste.getSrcText()
		def search
		def replac
		// pseudo-translate numbers
		search = /[0-9](?![^<]*?>)/
		replac = "#"
		pseudo = pseudo.replaceAll(search, replac)	
		// pseudo-translate upper cased letters
		search = /[A-ZÀÁÄÂÃÈÉËÊÌÍÏÎÒÓÖÔÕÚÙÜÛ](?![^<]*?>)/
		replac = "X"
		pseudo = pseudo.replaceAll(search, replac)		
		// pseudo-translate upper cased letters
		search = /[a-zàáäâãèéëêìíïîòóöôõùúüû](?![^<]*?>)/
		replac = "x"
		pseudo = pseudo.replaceAll(search, replac)		
		// write pseudo-translation		
		editor.replaceEditText(pseudo)
	}
}
*/

// Check
// [a-wyzA-WYZ0-9]
// [!-’,;.:“”\(\)\]\[\?&=xX#–"º%…—/   ^t^13  ]
