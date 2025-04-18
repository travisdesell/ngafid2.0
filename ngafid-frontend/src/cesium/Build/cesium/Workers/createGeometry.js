define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./AttributeCompression-6cb5b251", "./GeometryPipeline-99c06fbd", "./EncodedCartesian3-e19aab62", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./PrimitivePipeline-a7956312", "./WebMercatorProjection-44bf888f", "./createTaskProcessorWorker"], function (d, e, r, t, n, i, o, a, s, f, c, u, b, m, l, p, y, P, k) {
    "use strict";
    var v = {};

    function C(e) {
        var r = v[e];
        return d.defined(r) || ("object" == typeof exports ? v[r] = r = require("Workers/" + e) : require(["Workers/" + e], function (e) {
            v[r = e] = e
        })), r
    }

    return k(function (e, r) {
        for (var t = e.subTasks, n = t.length, i = new Array(n), o = 0; o < n; o++) {
            var a = t[o], s = a.geometry, f = a.moduleName;
            if (d.defined(f)) {
                var c = C(f);
                i[o] = c(s, a.offset)
            } else i[o] = s
        }
        return d.when.all(i, function (e) {
            return y.PrimitivePipeline.packCreateGeometryResults(e, r)
        })
    })
});
