define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed"], function (e, c, t, r) {
    "use strict";
    var h = r.CesiumMath.EPSILON10;
    e.arrayRemoveDuplicates = function (e, t, r) {
        if (c.defined(e)) {
            r = c.defaultValue(r, !1);
            var f, n, i, a = e.length;
            if (a < 2) return e;
            for (f = 1; f < a && !t(n = e[f - 1], i = e[f], h); ++f) ;
            if (f === a) return r && t(e[0], e[e.length - 1], h) ? e.slice(1) : e;
            for (var u = e.slice(0, f); f < a; ++f) t(n, i = e[f], h) || (u.push(i), n = i);
            return r && 1 < u.length && t(u[0], u[u.length - 1], h) && u.shift(), u
        }
    }
});
