var thymeleafLib = require('/lib/xp/thymeleaf');
var view = resolve('{{portletName}}.html');

function handleGet(req) {

    var params = {
        partName: "{{portletDisplayName}}"
    };

    var body = thymeleafLib.render(view, params);

    return {
        contentType: 'text/html',
        body: body
    };
}

exports.get = handleGet;