
/**
 * User: me@kipz.org
 * Date: 04/02/2013
 * Time: 17:04
 *
 * Config for medai2flickr script.
 *
 * This is groovy code format (we love this about groovy)
 */

media2flickr{
    imageTypes = ["jpg","jpeg", "png", "gif", "tiff", "tif"]
    videoTypes = ["avi","wmv","mov","mpeg","mpg","3gp","m2ts","ogg","ogv"]

    auth{
        key = "65bb099a6a779eee9a6e06a1bd942952"
        secret = "13f28866d1b43644"
        token = System.properties['user.home'] + "/.media2flickr.token"
    }
    collections = [
        [ "dir" : System.properties['user.home'] + "/Pictures/"]
    ]
}
