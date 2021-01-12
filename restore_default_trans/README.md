# Script: Remove Redundant ID-binding in TMs

<!--- https://imgur.com/gallery/8tIFPMG -->

## Purpose 

The purpose of these scripts is to carefully remove context metadata (e.g. ID-binding) in entries where it is not absolutely needed, so as not to hamper auto-propagation and proper flowing of the normal sorting of matches. For example, it is meant to solve the situation where many identical translations are in fact alternative translations, making it a pain to apply edits consistently (see the multiple translations pane in the screenshot below):

<!-- https://imgur.com/zbNMc2R -->
![Alt text](https://i.imgur.com/zbNMc2R.png "Multiple translations with identical text")



## Cases


### 1: Repeated segment with no default translation and several alternative translations

When a repeated segment has no default translation, it computes what is the most frequent of all the existing alternative translations and turns that into the default translation:

So here in segment 598, the translation “Teatud määral” is more frequent (2x) than the other two (1x each), 

![Alt text](https://i.imgur.com/ZanccOS.png "x")

and therefore becomes the default translation:

![Alt text](https://i.imgur.com/FRIdRo6.png "x")

<!--- test what happens with a unique segment with several alternative transaltion --> 


### 2: Unique segment with no default translation and one alternative translation only

When a unique segment only has one alternative translation, it removes the context properties so that it becomes a default translation.

So in cases like segment 16, the alternative translation

![Alt text](https://i.imgur.com/MzQGAoU.png "x")

becomes the default translation: 

![Alt text](https://i.imgur.com/SY6xLtX.png "x") 

<!--- test what happens with repeated segments with one alternative transaltion --> 


### 3: Repeated segment with several identical translations (one default and some alternative)

3: When a repeated segment has a default translation and one or more alternative translations that are identical to the default translation, it removes the identical alternative translations.

So for example repeated segment “Full time”, which has to one default translation and one alternative translation, which are identical:

![Alt text](https://i.imgur.com/7WScacQ.png "x") 

the alternative translation is deleted, so only the default translation remains:

![Alt text](https://i.imgur.com/RY70EuN.png "x") 

Likewise, if there are many alternative translations which are identical to the default translation,

![Alt text](https://i.imgur.com/C3UgLlI.png "x") 

all the alternative translations are removed and only the default translation is kept:

![Alt text](https://i.imgur.com/9MuTtz4.png "x") 


## Non-cases

Any alternative translations that are different from the default translation remain unaltered.

For example, segment “(Please select one response.)” has some alternative translations with text “(Palun vali üks vastus.)”, which is identical to the default translation. Those alternative translations are not necessary, so they are deleted:

![Alt text](https://i.imgur.com/rGkRqp7.png "x") 

Only the alternative translations (e.g. “(Palun valige üks vastus.”) which are different from the default translation are kept:

![Alt text](https://i.imgur.com/IjXUv95.png "x") 

Also, whenever removing context properties from the alternative translation to make it the default translation, the script checks the enforced matches. If there is an enforced match for the same source text with a different translation but there isn't one with the same translation, then the context properties must be kept to avoid the segment being pre-translated with a different translation coming from the enforced TM.
 
