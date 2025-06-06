define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./GeometryOffsetAttribute-e6e9672c", "./EllipseGeometryLibrary-de353ee5", "./EllipseOutlineGeometry-99f34d55"], function (l, e, i, n, t, r, s, o, a, d, u, c, m, p) {
    "use strict";

    function y(e) {
        var i = (e = l.defaultValue(e, l.defaultValue.EMPTY_OBJECT)).radius, t = {
            center: e.center,
            semiMajorAxis: i,
            semiMinorAxis: i,
            ellipsoid: e.ellipsoid,
            height: e.height,
            extrudedHeight: e.extrudedHeight,
            granularity: e.granularity,
            numberOfVerticalLines: e.numberOfVerticalLines
        };
        this._ellipseGeometry = new p.EllipseOutlineGeometry(t), this._workerName = "createCircleOutlineGeometry"
    }

    y.packedLength = p.EllipseOutlineGeometry.packedLength, y.pack = function (e, i, t) {
        return p.EllipseOutlineGeometry.pack(e._ellipseGeometry, i, t)
    };
    var f = new p.EllipseOutlineGeometry({center: new n.Cartesian3, semiMajorAxis: 1, semiMinorAxis: 1}), G = {
        center: new n.Cartesian3,
        radius: void 0,
        ellipsoid: n.Ellipsoid.clone(n.Ellipsoid.UNIT_SPHERE),
        height: void 0,
        extrudedHeight: void 0,
        granularity: void 0,
        numberOfVerticalLines: void 0,
        semiMajorAxis: void 0,
        semiMinorAxis: void 0
    };
    return y.unpack = function (e, i, t) {
        var r = p.EllipseOutlineGeometry.unpack(e, i, f);
        return G.center = n.Cartesian3.clone(r._center, G.center), G.ellipsoid = n.Ellipsoid.clone(r._ellipsoid, G.ellipsoid), G.height = r._height, G.extrudedHeight = r._extrudedHeight, G.granularity = r._granularity, G.numberOfVerticalLines = r._numberOfVerticalLines, l.defined(t) ? (G.semiMajorAxis = r._semiMajorAxis, G.semiMinorAxis = r._semiMinorAxis, t._ellipseGeometry = new p.EllipseOutlineGeometry(G), t) : (G.radius = r._semiMajorAxis, new y(G))
    }, y.createGeometry = function (e) {
        return p.EllipseOutlineGeometry.createGeometry(e._ellipseGeometry)
    }, function (e, i) {
        return l.defined(i) && (e = y.unpack(e, i)), e._ellipseGeometry._center = n.Cartesian3.clone(e._ellipseGeometry._center), e._ellipseGeometry._ellipsoid = n.Ellipsoid.clone(e._ellipseGeometry._ellipsoid), y.createGeometry(e)
    }
});
