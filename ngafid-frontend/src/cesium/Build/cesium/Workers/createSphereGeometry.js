define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./IndexDatatype-e3260434", "./GeometryOffsetAttribute-e6e9672c", "./VertexFormat-ad523db1", "./EllipsoidGeometry-3c001161"], function (a, e, t, o, r, i, n, s, d, c, l, m, u, p) {
    "use strict";

    function y(e) {
        var t = a.defaultValue(e.radius, 1), r = {
            radii: new o.Cartesian3(t, t, t),
            stackPartitions: e.stackPartitions,
            slicePartitions: e.slicePartitions,
            vertexFormat: e.vertexFormat
        };
        this._ellipsoidGeometry = new p.EllipsoidGeometry(r), this._workerName = "createSphereGeometry"
    }

    y.packedLength = p.EllipsoidGeometry.packedLength, y.pack = function (e, t, r) {
        return p.EllipsoidGeometry.pack(e._ellipsoidGeometry, t, r)
    };
    var G = new p.EllipsoidGeometry, f = {
        radius: void 0,
        radii: new o.Cartesian3,
        vertexFormat: new u.VertexFormat,
        stackPartitions: void 0,
        slicePartitions: void 0
    };
    return y.unpack = function (e, t, r) {
        var i = p.EllipsoidGeometry.unpack(e, t, G);
        return f.vertexFormat = u.VertexFormat.clone(i._vertexFormat, f.vertexFormat), f.stackPartitions = i._stackPartitions, f.slicePartitions = i._slicePartitions, a.defined(r) ? (o.Cartesian3.clone(i._radii, f.radii), r._ellipsoidGeometry = new p.EllipsoidGeometry(f), r) : (f.radius = i._radii.x, new y(f))
    }, y.createGeometry = function (e) {
        return p.EllipsoidGeometry.createGeometry(e._ellipsoidGeometry)
    }, function (e, t) {
        return a.defined(t) && (e = y.unpack(e, t)), y.createGeometry(e)
    }
});
