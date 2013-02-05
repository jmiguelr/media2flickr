
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
        key = "xx"
        secret = "xx"
        token = System.properties['user.home'] + "/.media2flickr.token"
    }
    collections = [
        [ "dir" : System.properties['user.home'] + "/Pictures/"]
    ]
}