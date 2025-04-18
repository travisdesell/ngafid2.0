define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./AttributeCompression-6cb5b251", "./GeometryPipeline-99c06fbd", "./EncodedCartesian3-e19aab62", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./GeometryOffsetAttribute-e6e9672c", "./VertexFormat-ad523db1", "./EllipseGeometryLibrary-de353ee5", "./GeometryInstance-b43ca1c5", "./EllipseGeometry-7d13646a"], function (o, e, t, n, i, r, a, s, l, d, m, c, u, p, y, _, h, G, x, f, g) {
    "use strict";

    function v(e) {
        var t = (e = o.defaultValue(e, o.defaultValue.EMPTY_OBJECT)).radius, i = {
            center: e.center,
            semiMajorAxis: t,
            semiMinorAxis: t,
            ellipsoid: e.ellipsoid,
            height: e.height,
            extrudedHeight: e.extrudedHeight,
            granularity: e.granularity,
            vertexFormat: e.vertexFormat,
            stRotation: e.stRotation,
            shadowVolume: e.shadowVolume
        };
        this._ellipseGeometry = new g.EllipseGeometry(i), this._workerName = "createCircleGeometry"
    }

    v.packedLength = g.EllipseGeometry.packedLength, v.pack = function (e, t, i) {
        return g.EllipseGeometry.pack(e._ellipseGeometry, t, i)
    };
    var E = new g.EllipseGeometry({center: new n.Cartesian3, semiMajorAxis: 1, semiMinorAxis: 1}), w = {
        center: new n.Cartesian3,
        radius: void 0,
        ellipsoid: n.Ellipsoid.clone(n.Ellipsoid.UNIT_SPHERE),
        height: void 0,
        extrudedHeight: void 0,
        granularity: void 0,
        vertexFormat: new G.VertexFormat,
        stRotation: void 0,
        semiMajorAxis: void 0,
        semiMinorAxis: void 0,
        shadowVolume: void 0
    };
    return v.unpack = function (e, t, i) {
        var r = g.EllipseGeometry.unpack(e, t, E);
        return w.center = n.Cartesian3.clone(r._center, w.center), w.ellipsoid = n.Ellipsoid.clone(r._ellipsoid, w.ellipsoid), w.height = r._height, w.extrudedHeight = r._extrudedHeight, w.granularity = r._granularity, w.vertexFormat = G.VertexFormat.clone(r._vertexFormat, w.vertexFormat), w.stRotation = r._stRotation, w.shadowVolume = r._shadowVolume, o.defined(i) ? (w.semiMajorAxis = r._semiMajorAxis, w.semiMinorAxis = r._semiMinorAxis, i._ellipseGeometry = new g.EllipseGeometry(w), i) : (w.radius = r._semiMajorAxis, new v(w))
    }, v.createGeometry = function (e) {
        return g.EllipseGeometry.createGeometry(e._ellipseGeometry)
    }, v.createShadowVolume = function (e, t, i) {
        var r = e._ellipseGeometry._granularity, o = e._ellipseGeometry._ellipsoid, n = t(r, o), a = i(r, o);
        return new v({
            center: e._ellipseGeometry._center,
            radius: e._ellipseGeometry._semiMajorAxis,
            ellipsoid: o,
            stRotation: e._ellipseGeometry._stRotation,
            granularity: r,
            extrudedHeight: n,
            height: a,
            vertexFormat: G.VertexFormat.POSITION_ONLY,
            shadowVolume: !0
        })
    }, Object.defineProperties(v.prototype, {
        rectangle: {
            get: function () {
                return this._ellipseGeometry.rectangle
            }
        }, textureCoordinateRotationPoints: {
            get: function () {
                return this._ellipseGeometry.textureCoordinateRotationPoints
            }
        }
    }), function (e, t) {
        return o.defined(t) && (e = v.unpack(e, t)), e._ellipseGeometry._center = n.Cartesian3.clone(e._ellipseGeometry._center), e._ellipseGeometry._ellipsoid = n.Ellipsoid.clone(e._ellipseGeometry._ellipsoid), v.createGeometry(e)
    }
});
