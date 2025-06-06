define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02"], function (n, i, e, a, o, t) {
    "use strict";

    function s(n, e) {
        this.normal = o.Cartesian3.clone(n), this.distance = e
    }

    s.fromPointNormal = function (n, e, a) {
        var t = -o.Cartesian3.dot(e, n);
        return i.defined(a) ? (o.Cartesian3.clone(e, a.normal), a.distance = t, a) : new s(e, t)
    };
    var r = new o.Cartesian3;
    s.fromCartesian4 = function (n, e) {
        var a = o.Cartesian3.fromCartesian4(n, r), t = n.w;
        return i.defined(e) ? (o.Cartesian3.clone(a, e.normal), e.distance = t, e) : new s(a, t)
    }, s.getPointDistance = function (n, e) {
        return o.Cartesian3.dot(n.normal, e) + n.distance
    };
    var c = new o.Cartesian3;
    s.projectPointOntoPlane = function (n, e, a) {
        i.defined(a) || (a = new o.Cartesian3);
        var t = s.getPointDistance(n, e), r = o.Cartesian3.multiplyByScalar(n.normal, t, c);
        return o.Cartesian3.subtract(e, r, a)
    };
    var l = new o.Cartesian3;
    s.transform = function (n, e, a) {
        return t.Matrix4.multiplyByPointAsVector(e, n.normal, r), o.Cartesian3.normalize(r, r), o.Cartesian3.multiplyByScalar(n.normal, -n.distance, l), t.Matrix4.multiplyByPoint(e, l, l), s.fromPointNormal(l, r, a)
    }, s.clone = function (n, e) {
        return i.defined(e) ? (o.Cartesian3.clone(n.normal, e.normal), e.distance = n.distance, e) : new s(n.normal, n.distance)
    }, s.equals = function (n, e) {
        return n.distance === e.distance && o.Cartesian3.equals(n.normal, e.normal)
    }, s.ORIGIN_XY_PLANE = Object.freeze(new s(o.Cartesian3.UNIT_Z, 0)), s.ORIGIN_YZ_PLANE = Object.freeze(new s(o.Cartesian3.UNIT_X, 0)), s.ORIGIN_ZX_PLANE = Object.freeze(new s(o.Cartesian3.UNIT_Y, 0)), n.Plane = s
});
