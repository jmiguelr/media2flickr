//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

import com.aetrion.flickr.Flickr
import com.aetrion.flickr.REST
import com.aetrion.flickr.RequestContext
import com.aetrion.flickr.auth.Permission
import com.aetrion.flickr.photos.Photo
import com.aetrion.flickr.photos.SearchParameters
import com.aetrion.flickr.tags.Tag
import com.aetrion.flickr.uploader.UploadMetaData

@Grab(group = 'com.aetrion.flickr', module = 'flickrapi', version = '1.1')
import java.security.MessageDigest

/**
 * Uploads all pictures/videos in a set of directories to flickr, adding a media2flickr tag. If you remove this from flickr, media2flickr
 * will try to upload it again.
 * Each media uploaded with medai2flickr will have a media2flicker:<hashcode> tag. Local photos that have the same hashcode
 * are not uploaded again.
 *
 * Flickr is the source of truth. Media is ever downloaded from flickr. The local filesystems are never modified or touched
 * in anyway. Nothing is ever removed from flickr.
 *
 * Folder names are added as tags.
 * */

def pub = new Publisher("conf.groovy")

def fhashes = pub.getFlickrHashes()

pub.files { File root, File file ->
    def hash = pub.hash(file)
    if (!fhashes.contains(hash)) {
        def tags = []
        File current = file
        while(current.getName() != root.getName()){
            tags << current.getName()
            current = current.getParentFile()
        }
        pub.publish(file, tags, hash)
    } else {
        pub.log "Not uploading file: " + file.getAbsolutePath() + " as flickr already has it with hash: " + hash
    }
}
/**
 * Put everything in a class because script scope is a pain
 */
class Publisher {

    /**
     * Used to identify photos uploaded by media2flickr
     */
    static String TAG_PREFIX = "media2flickr"

    /**
     * Slurped config
     */
    ConfigObject config
    /**
     * Flickrj client
     */
    Flickr flickr

    Publisher(String configFile) {
        //slurp config
        config = new ConfigSlurper().parse(new File(configFile).toURL()).media2flickr
        def flickr_key = config.auth.key
        def flickr_secret = config.auth.secret
        def tokenFile = new File(config.auth.token)
        flickr = new Flickr(flickr_key, flickr_secret, new REST())
        def authInterface = flickr.getAuthInterface()
        // go through auth
        if (tokenFile.canRead()) {
            def token = tokenFile.readLines()[0]
            log "Checking token in file: " + tokenFile
            def auth = authInterface.checkToken(token)
            RequestContext.getRequestContext().setAuth(auth);
            log "Using token from: " + tokenFile
        } else {
            def frob = authInterface.getFrob()
            def url = authInterface.buildAuthenticationUrl(Permission.WRITE, frob);
            log "Copy and paste the following URL in to your browser to authorise media2flickr:"
            log url.toString()
            log "Press enter when done."
            System.in.withReader {
                it.readLine()
            }
            def auth = authInterface.getToken(frob)
            RequestContext.getRequestContext().setAuth(auth);
            def token = auth.getToken()
            assert tokenFile.createNewFile()
            log "Saving token to: " + tokenFile
            tokenFile << token
        }
    }

    /**
     * Publish a file to flickr and set appropriate tags, perms etc
     * @param media
     */
    def publish(File media, List<String> tags, String hash) {
        log "Uploading file: " + media.getAbsolutePath() + " (${hash})..."
        def meta = new UploadMetaData()
        meta.setAsync(false)
        meta.setHidden(true)
        meta.setPublicFlag(false)
        meta.setFriendFlag(false)
        meta.setFamilyFlag(false)
        tags << TAG_PREFIX
        tags << TAG_PREFIX + hash
        meta.setTags(tags)
        meta.setTitle(media.getName())
        flickr.getUploader().upload(new FileInputStream(media), meta)
        //log media.getAbsolutePath() +" uploaded to Flickr"
    }
    /**
     * Get all hashes of the photos on flickr that media2flickr knows about
     * @return
     */
    List<String> getFlickrHashes() {
        log "Retrieving hashes of all photos on Flickr..."
        def hashes = [] as List<String>
        photos { Photo photo ->
            photo.getTags().each { Tag tagob ->
                String tag = tagob.value
                if (tag.startsWith(TAG_PREFIX) && tag.length() > TAG_PREFIX.length()) {
                    hashes << tag - TAG_PREFIX
                }
            }
        }
        log "Retrieved hashes of ${hashes.size()} photos"
        hashes
    }
/**
 * Perform "operation" on each flickr media item owned by the user and uploaded by media2flickr
 */
    def photos(Closure operation) {
        def params = new SearchParameters()
        params.setTags([TAG_PREFIX] as String[])
        params.setExtrasTags(true)
        params.setUserId("me")
        def page = 0
        def photos = flickr.photosInterface.search(params, 500, ++page)
        photos.each { Photo photo ->
            operation.call(photo)
        }
        def total = photos.getTotal()
        def sofar = photos.size()

        while (sofar < total) {
            photos = flickr.photosInterface.search(params, 500, ++page)
            sofar += photos.size()
            photos.each { Photo photo ->
                operation.call(photo)
            }
        }
    }
    /**
     * Like below, but does it for all directories in the config
     * @param operation
     */
    def files(Closure operation) {
        config.collections.each { Map<String, String> collection ->
            def parent = new File(collection["dir"])
            filesInDirectory(parent, parent, operation)
        }
    }
/**
 * Perform "operation" on each file in a directory recursively
 */
    private def filesInDirectory(File parent, File directory, Closure operation) {
        directory.eachDir { File subdir ->
            filesInDirectory(parent,subdir, operation)
        }
        directory.eachFile { File file ->
            if (isAllowed(file)) {
                operation.call(parent, file)
            }
        }
        if (isAllowed(directory)) {
            operation.call(parent, directory)
        }
    }
    /**
     * Returns true if flickr will accept this type of file
     * @param file
     * @return
     */
    boolean isAllowed(File file) {
        if (file.isFile()) {

            def path = file.getAbsolutePath()
            if (path.contains(".")) {
                def ext = path.substring(path.lastIndexOf(".") + 1, path.length()).toLowerCase()
                if (config.imageTypes.contains(ext) || config.videoTypes.contains(ext)) {
                    return true
                }
            }
        }
        false
    }
/**
 * Calculate the hash of a file
 * @param f
 */
    def static hash(File f) {
        int KB = 1024
        int MB = 1024 * KB
        if (!f.exists() || !f.isFile()) {
            throw new RuntimeException("Invalid file $f provided")
        }
        def messageDigest = MessageDigest.getInstance("SHA1")
        f.eachByte(MB) { byte[] buf, int bytesRead ->
            messageDigest.update(buf, 0, bytesRead);
        }
        new BigInteger(1, messageDigest.digest()).toString(16).padLeft(40, '0')
    }

    def static succeed(String str) {
        log(str)
        System.exit(0)
    }

    def static log(String str) {
        System.out.println(str)
    }

    def static fail(String str) {
        System.err.println(str)
        System.exit(1)
    }
}





