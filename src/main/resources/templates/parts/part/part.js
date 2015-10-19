var portal = require('/lib/xp/portal');
var thymeleaf = require('/lib/xp/thymeleaf');
var view = resolve('{{portletName}}.html');

function handleGet(req) {

    var getSite = portal.getSite();

    var params = {
        partName: "{{portletDisplayName}}"
    };

    var body = thymeleaf.render(view, params);

    return {
        contentType: 'text/html',
        body: body
    };
}

exports.get = handleGet;