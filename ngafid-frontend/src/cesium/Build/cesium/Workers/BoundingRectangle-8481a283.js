define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Cartesian2-e875d9d2", "./Transforms-44592b02"], function (t, o, e, r, a) {
    "use strict";

    function x(t, e, n, i) {
        this.x = o.defaultValue(t, 0), this.y = o.defaultValue(e, 0), this.width = o.defaultValue(n, 0), this.height = o.defaultValue(i, 0)
    }

    x.packedLength = 4, x.pack = function (t, e, n) {
        return n = o.defaultValue(n, 0), e[n++] = t.x, e[n++] = t.y, e[n++] = t.width, e[n] = t.height, e
    }, x.unpack = function (t, e, n) {
        return e = o.defaultValue(e, 0), o.defined(n) || (n = new x), n.x = t[e++], n.y = t[e++], n.width = t[e++], n.height = t[e], n
    }, x.fromPoints = function (t, e) {
        if (o.defined(e) || (e = new x), !o.defined(t) || 0 === t.length) return e.x = 0, e.y = 0, e.width = 0, e.height = 0, e;
        for (var n = t.length, i = t[0].x, h = t[0].y, r = t[0].x, a = t[0].y, d = 1; d < n; d++) {
            var u = t[d], c = u.x, f = u.y;
            i = Math.min(c, i), r = Math.max(c, r), h = Math.min(f, h), a = Math.max(f, a)
        }
        return e.x = i, e.y = h, e.width = r - i, e.height = a - h, e
    };
    var d = new a.GeographicProjection, u = new r.Cartographic, c = new r.Cartographic;
    x.fromRectangle = function (t, e, n) {
        if (o.defined(n) || (n = new x), !o.defined(t)) return n.x = 0, n.y = 0, n.width = 0, n.height = 0, n;
        var i = (e = o.defaultValue(e, d)).project(r.Rectangle.southwest(t, u)),
            h = e.project(r.Rectangle.northeast(t, c));
        return r.Cartesian2.subtract(h, i, h), n.x = i.x, n.y = i.y, n.width = h.x, n.height = h.y, n
    }, x.clone = function (t, e) {
        if (o.defined(t)) return o.defined(e) ? (e.x = t.x, e.y = t.y, e.width = t.width, e.height = t.height, e) : new x(t.x, t.y, t.width, t.height)
    }, x.union = function (t, e, n) {
        o.defined(n) || (n = new x);
        var i = Math.min(t.x, e.x), h = Math.min(t.y, e.y), r = Math.max(t.x + t.width, e.x + e.width),
            a = Math.max(t.y + t.height, e.y + e.height);
        return n.x = i, n.y = h, n.width = r - i, n.height = a - h, n
    }, x.expand = function (t, e, n) {
        n = x.clone(t, n);
        var i = e.x - n.x, h = e.y - n.y;
        return i > n.width ? n.width = i : i < 0 && (n.width -= i, n.x = e.x), h > n.height ? n.height = h : h < 0 && (n.height -= h, n.y = e.y), n
    }, x.intersect = function (t, e) {
        var n = t.x, i = t.y, h = e.x, r = e.y;
        return n > h + e.width || n + t.width < h || i + t.height < r || i > r + e.height ? a.Intersect.OUTSIDE : a.Intersect.INTERSECTING
    }, x.equals = function (t, e) {
        return t === e || o.defined(t) && o.defined(e) && t.x === e.x && t.y === e.y && t.width === e.width && t.height === e.height
    }, x.prototype.clone = function (t) {
        return x.clone(this, t)
    }, x.prototype.intersect = function (t) {
        return x.intersect(this, t)
    }, x.prototype.equals = function (t) {
        return x.equals(this, t)
    }, t.BoundingRectangle = x
});
