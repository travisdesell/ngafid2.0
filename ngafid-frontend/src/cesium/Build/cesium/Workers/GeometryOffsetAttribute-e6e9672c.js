define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc"], function (e, c, t) {
    "use strict";
    var f = Object.freeze({NONE: 0, TOP: 1, ALL: 2});
    e.GeometryOffsetAttribute = f, e.arrayFill = function (e, t, f, a) {
        if ("function" == typeof e.fill) return e.fill(t, f, a);
        for (var r = e.length >>> 0, n = c.defaultValue(f, 0), i = n < 0 ? Math.max(r + n, 0) : Math.min(n, r), l = c.defaultValue(a, r), u = l < 0 ? Math.max(r + l, 0) : Math.min(l, r); i < u;) e[i] = t, i++;
        return e
    }
});
