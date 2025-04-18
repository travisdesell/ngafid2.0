define(["exports", "./Check-c4f3a3fc", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./OrientedBoundingBox-e6450288"], function (n, t, l, x, B) {
    "use strict";
    var e = {}, s = new l.Cartesian3, P = new l.Cartesian3, M = new l.Cartesian3, h = new l.Cartesian3,
        v = new B.OrientedBoundingBox;

    function o(n, t, e, r, a) {
        var i = l.Cartesian3.subtract(n, t, s), o = l.Cartesian3.dot(e, i), u = l.Cartesian3.dot(r, i);
        return l.Cartesian2.fromElements(o, u, a)
    }

    e.validOutline = function (n) {
        var t = B.OrientedBoundingBox.fromPoints(n, v).halfAxes, e = x.Matrix3.getColumn(t, 0, P),
            r = x.Matrix3.getColumn(t, 1, M), a = x.Matrix3.getColumn(t, 2, h), i = l.Cartesian3.magnitude(e),
            o = l.Cartesian3.magnitude(r), u = l.Cartesian3.magnitude(a);
        return !(0 === i && (0 === o || 0 === u) || 0 === o && 0 === u)
    }, e.computeProjectTo2DArguments = function (n, t, e, r) {
        var a, i, o = B.OrientedBoundingBox.fromPoints(n, v), u = o.halfAxes, s = x.Matrix3.getColumn(u, 0, P),
            C = x.Matrix3.getColumn(u, 1, M), c = x.Matrix3.getColumn(u, 2, h), m = l.Cartesian3.magnitude(s),
            d = l.Cartesian3.magnitude(C), g = l.Cartesian3.magnitude(c), f = Math.min(m, d, g);
        return (0 !== m || 0 !== d && 0 !== g) && (0 !== d || 0 !== g) && (f !== d && f !== g || (a = s), f === m ? a = C : f === g && (i = C), f !== m && f !== d || (i = c), l.Cartesian3.normalize(a, e), l.Cartesian3.normalize(i, r), l.Cartesian3.clone(o.center, t), !0)
    }, e.createProjectPointsTo2DFunction = function (r, a, i) {
        return function (n) {
            for (var t = new Array(n.length), e = 0; e < n.length; e++) t[e] = o(n[e], r, a, i);
            return t
        }
    }, e.createProjectPointTo2DFunction = function (e, r, a) {
        return function (n, t) {
            return o(n, e, r, a, t)
        }
    }, n.CoplanarPolygonGeometryLibrary = e
});
