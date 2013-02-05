# media2flickr #

Use this Groovy script to upload arbitrary photo/video folder structures to your flickr account

Contact 'me at kipz.org' if you want bug fixes and/or features.

## Features ##

* No state (except config) is stored by this script (this was imortant for my use case)
* All uploaded items are tagged with "media2flickr". This is used by this script. If you delete this tag, the item will try to re-upload it
* Each item uploaded will have a media2flicker:<hashcode> tag. As above, don't remove this if you don't want the script to try and upload the item again.
* Local photos that have the same hashcode are not uploaded again
* Flickr is the source of truth. Media is never downloaded from Flickr. Media is never deleted/modifed on Flickr or the local filesystem.
* Folder names below the root are added as tags
* Uploaded items are private and hidden (i.e. not friends or family)

## Instructions ##
* Make sure you have [Groovy 2.x](http://groovy.codehaus.org/) installed
* Edit the config file (conf.groovy) to your needs. It should be self explanatory for geeky types.
** You probably only need to set the key/secret and the directory to upload in "collections"
* Run the script

    bash> groovy Media2Flickr.groovy

* Follow the instructions on your console to authenticate Media2Flickr with Flickr
* Sit back and enjoy

## TODO ##
* Create sets based folder structures.
* Ability to set privacy level in config (I don't need this yet)






