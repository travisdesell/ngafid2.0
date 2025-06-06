define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./GeometryPipeline-99c06fbd", "./IndexDatatype-e3260434", "./WebMercatorProjection-44bf888f"], function (e, v, t, u, P, k, C, w, b, A, c) {
    "use strict";

    function m(e, t, r) {
        e = v.defaultValue(e, 0), t = v.defaultValue(t, 0), r = v.defaultValue(r, 0), this.value = new Float32Array([e, t, r])
    }

    function G(e, t) {
        var r = e.attributes, n = r.position, i = n.values.length / n.componentsPerAttribute;
        r.batchId = new C.GeometryAttribute({
            componentDatatype: k.ComponentDatatype.FLOAT,
            componentsPerAttribute: 1,
            values: new Float32Array(i)
        });
        for (var o = r.batchId.values, a = 0; a < i; ++a) o[a] = t
    }

    function x(e) {
        var t, r, n = e.instances, i = e.projection, o = e.elementIndexUintSupported, a = e.scene3DOnly,
            s = e.vertexCacheOptimize, d = e.compressVertices, p = e.modelMatrix, f = n.length;
        for (t = 0; t < f; ++t) if (v.defined(n[t].geometry)) {
            n[t].geometry.primitiveType;
            break
        }
        if (!function (e, t, r) {
            var n, i = !r, o = e.length;
            if (!i && 1 < o) {
                var a = e[0].modelMatrix;
                for (n = 1; n < o; ++n) if (!P.Matrix4.equals(a, e[n].modelMatrix)) {
                    i = !0;
                    break
                }
            }
            if (i) for (n = 0; n < o; ++n) v.defined(e[n].geometry) && b.GeometryPipeline.transformToWorldCoordinates(e[n]); else P.Matrix4.multiplyTransformation(t, e[0].modelMatrix, t)
        }(n, p, a), !a) for (t = 0; t < f; ++t) v.defined(n[t].geometry) && b.GeometryPipeline.splitLongitude(n[t]);
        if (!function (e) {
            for (var t = e.length, r = 0; r < t; ++r) {
                var n = e[r];
                v.defined(n.geometry) ? G(n.geometry, r) : v.defined(n.westHemisphereGeometry) && v.defined(n.eastHemisphereGeometry) && (G(n.westHemisphereGeometry, r), G(n.eastHemisphereGeometry, r))
            }
        }(n), s) for (t = 0; t < f; ++t) {
            var u = n[t];
            v.defined(u.geometry) ? (b.GeometryPipeline.reorderForPostVertexCache(u.geometry), b.GeometryPipeline.reorderForPreVertexCache(u.geometry)) : v.defined(u.westHemisphereGeometry) && v.defined(u.eastHemisphereGeometry) && (b.GeometryPipeline.reorderForPostVertexCache(u.westHemisphereGeometry), b.GeometryPipeline.reorderForPreVertexCache(u.westHemisphereGeometry), b.GeometryPipeline.reorderForPostVertexCache(u.eastHemisphereGeometry), b.GeometryPipeline.reorderForPreVertexCache(u.eastHemisphereGeometry))
        }
        var c = b.GeometryPipeline.combineInstances(n);
        for (f = c.length, t = 0; t < f; ++t) {
            var m, h = (r = c[t]).attributes;
            if (a) for (m in h) h.hasOwnProperty(m) && h[m].componentDatatype === k.ComponentDatatype.DOUBLE && b.GeometryPipeline.encodeAttribute(r, m, m + "3DHigh", m + "3DLow"); else for (m in h) if (h.hasOwnProperty(m) && h[m].componentDatatype === k.ComponentDatatype.DOUBLE) {
                var l = m + "3D", g = m + "2D";
                b.GeometryPipeline.projectTo2D(r, m, l, g, i), v.defined(r.boundingSphere) && "position" === m && (r.boundingSphereCV = P.BoundingSphere.fromVertices(r.attributes.position2D.values)), b.GeometryPipeline.encodeAttribute(r, l, l + "High", l + "Low"), b.GeometryPipeline.encodeAttribute(r, g, g + "High", g + "Low")
            }
            d && b.GeometryPipeline.compressVertices(r)
        }
        if (!o) {
            var y = [];
            for (f = c.length, t = 0; t < f; ++t) r = c[t], y = y.concat(b.GeometryPipeline.fitToUnsignedShortIndices(r));
            c = y
        }
        return c
    }

    function S(e, t, r, n) {
        var i, o, a, s = n.length - 1;
        if (0 <= s) {
            var d = n[s];
            i = d.offset + d.count, o = r[a = d.index].indices.length
        } else o = r[a = i = 0].indices.length;
        for (var p = e.length, f = 0; f < p; ++f) {
            var u = e[f][t];
            if (v.defined(u)) {
                var c = u.indices.length;
                o < i + c && (i = 0, o = r[++a].indices.length), n.push({index: a, offset: i, count: c}), i += c
            }
        }
    }

    Object.defineProperties(m.prototype, {
        componentDatatype: {
            get: function () {
                return k.ComponentDatatype.FLOAT
            }
        }, componentsPerAttribute: {
            get: function () {
                return 3
            }
        }, normalize: {
            get: function () {
                return !1
            }
        }
    }), m.fromCartesian3 = function (e) {
        return new m(e.x, e.y, e.z)
    }, m.toValue = function (e, t) {
        return v.defined(t) || (t = new Float32Array([e.x, e.y, e.z])), t[0] = e.x, t[1] = e.y, t[2] = e.z, t
    };
    var h = {};

    function i(e, t) {
        var r = e.attributes;
        for (var n in r) if (r.hasOwnProperty(n)) {
            var i = r[n];
            v.defined(i) && v.defined(i.values) && t.push(i.values.buffer)
        }
        v.defined(e.indices) && t.push(e.indices.buffer)
    }

    function o(e) {
        var t = e.length, r = 1 + (P.BoundingSphere.packedLength + 1) * t, n = new Float32Array(r), i = 0;
        n[i++] = t;
        for (var o = 0; o < t; ++o) {
            var a = e[o];
            v.defined(a) ? (n[i++] = 1, P.BoundingSphere.pack(e[o], n, i)) : n[i++] = 0, i += P.BoundingSphere.packedLength
        }
        return n
    }

    function r(e) {
        for (var t = new Array(e[0]), r = 0, n = 1; n < e.length;) 1 === e[n++] && (t[r] = P.BoundingSphere.unpack(e, n)), ++r, n += P.BoundingSphere.packedLength;
        return t
    }

    h.combineGeometry = function (e) {
        var t, r, n, i, o, a, s, d = e.instances, p = d.length, f = !1;
        0 < p && (0 < (t = x(e)).length && (r = b.GeometryPipeline.createAttributeLocations(t[0]), e.createPickOffsets && (S(o = d, "geometry", a = t, s = []), S(o, "westHemisphereGeometry", a, s), S(o, "eastHemisphereGeometry", a, s), n = s)), v.defined(d[0].attributes) && v.defined(d[0].attributes.offset) && (i = new Array(p), f = !0));
        for (var u = new Array(p), c = new Array(p), m = 0; m < p; ++m) {
            var h = d[m], l = h.geometry;
            v.defined(l) && (u[m] = l.boundingSphere, c[m] = l.boundingSphereCV, f && (i[m] = h.geometry.offsetAttribute));
            var g = h.eastHemisphereGeometry, y = h.westHemisphereGeometry;
            v.defined(g) && v.defined(y) && (v.defined(g.boundingSphere) && v.defined(y.boundingSphere) && (u[m] = P.BoundingSphere.union(g.boundingSphere, y.boundingSphere)), v.defined(g.boundingSphereCV) && v.defined(y.boundingSphereCV) && (c[m] = P.BoundingSphere.union(g.boundingSphereCV, y.boundingSphereCV)))
        }
        return {
            geometries: t,
            modelMatrix: e.modelMatrix,
            attributeLocations: r,
            pickOffsets: n,
            offsetInstanceExtend: i,
            boundingSpheres: u,
            boundingSpheresCV: c
        }
    }, h.packCreateGeometryResults = function (e, t) {
        var r = new Float64Array(function (e) {
            for (var t = 1, r = e.length, n = 0; n < r; n++) {
                var i = e[n];
                if (++t, v.defined(i)) {
                    var o = i.attributes;
                    for (var a in t += 7 + 2 * P.BoundingSphere.packedLength + (v.defined(i.indices) ? i.indices.length : 0), o) {
                        if (o.hasOwnProperty(a) && v.defined(o[a])) t += 5 + o[a].values.length
                    }
                }
            }
            return t
        }(e)), n = [], i = {}, o = e.length, a = 0;
        r[a++] = o;
        for (var s = 0; s < o; s++) {
            var d = e[s], p = v.defined(d);
            if (r[a++] = p ? 1 : 0, p) {
                r[a++] = d.primitiveType, r[a++] = d.geometryType, r[a++] = v.defaultValue(d.offsetAttribute, -1);
                var f = v.defined(d.boundingSphere) ? 1 : 0;
                (r[a++] = f) && P.BoundingSphere.pack(d.boundingSphere, r, a), a += P.BoundingSphere.packedLength;
                var u = v.defined(d.boundingSphereCV) ? 1 : 0;
                (r[a++] = u) && P.BoundingSphere.pack(d.boundingSphereCV, r, a), a += P.BoundingSphere.packedLength;
                var c = d.attributes, m = [];
                for (var h in c) c.hasOwnProperty(h) && v.defined(c[h]) && (m.push(h), v.defined(i[h]) || (i[h] = n.length, n.push(h)));
                r[a++] = m.length;
                for (var l = 0; l < m.length; l++) {
                    var g = m[l], y = c[g];
                    r[a++] = i[g], r[a++] = y.componentDatatype, r[a++] = y.componentsPerAttribute, r[a++] = y.normalize ? 1 : 0, r[a++] = y.values.length, r.set(y.values, a), a += y.values.length
                }
                var b = v.defined(d.indices) ? d.indices.length : 0;
                0 < (r[a++] = b) && (r.set(d.indices, a), a += b)
            }
        }
        return t.push(r.buffer), {stringTable: n, packedData: r}
    }, h.unpackCreateGeometryResults = function (e) {
        for (var t, r = e.stringTable, n = e.packedData, i = new Array(n[0]), o = 0, a = 1; a < n.length;) {
            if (1 === n[a++]) {
                var s, d, p, f, u, c = n[a++], m = n[a++], h = n[a++];
                -1 === h && (h = void 0), 1 === n[a++] && (s = P.BoundingSphere.unpack(n, a)), a += P.BoundingSphere.packedLength, 1 === n[a++] && (d = P.BoundingSphere.unpack(n, a)), a += P.BoundingSphere.packedLength;
                var l, g = new w.GeometryAttributes, y = n[a++];
                for (t = 0; t < y; t++) {
                    var b = r[n[a++]], v = n[a++];
                    u = n[a++];
                    var G = 0 !== n[a++];
                    p = n[a++], f = k.ComponentDatatype.createTypedArray(v, p);
                    for (var x = 0; x < p; x++) f[x] = n[a++];
                    g[b] = new C.GeometryAttribute({
                        componentDatatype: v,
                        componentsPerAttribute: u,
                        normalize: G,
                        values: f
                    })
                }
                if (0 < (p = n[a++])) {
                    var S = f.length / u;
                    for (l = A.IndexDatatype.createTypedArray(S, p), t = 0; t < p; t++) l[t] = n[a++]
                }
                i[o++] = new C.Geometry({
                    primitiveType: c,
                    geometryType: m,
                    boundingSphere: s,
                    boundingSphereCV: d,
                    indices: l,
                    attributes: g,
                    offsetAttribute: h
                })
            } else i[o++] = void 0
        }
        return i
    }, h.packCombineGeometryParameters = function (e, t) {
        for (var r = e.createGeometryResults, n = r.length, i = 0; i < n; i++) t.push(r[i].packedData.buffer);
        return {
            createGeometryResults: e.createGeometryResults,
            packedInstances: function (e, t) {
                var r = e.length, n = new Float64Array(1 + 19 * r), i = 0;
                n[i++] = r;
                for (var o = 0; o < r; o++) {
                    var a = e[o];
                    if (P.Matrix4.pack(a.modelMatrix, n, i), i += P.Matrix4.packedLength, v.defined(a.attributes) && v.defined(a.attributes.offset)) {
                        var s = a.attributes.offset.value;
                        n[i] = s[0], n[i + 1] = s[1], n[i + 2] = s[2]
                    }
                    i += 3
                }
                return t.push(n.buffer), n
            }(e.instances, t),
            ellipsoid: e.ellipsoid,
            isGeographic: e.projection instanceof P.GeographicProjection,
            elementIndexUintSupported: e.elementIndexUintSupported,
            scene3DOnly: e.scene3DOnly,
            vertexCacheOptimize: e.vertexCacheOptimize,
            compressVertices: e.compressVertices,
            modelMatrix: e.modelMatrix,
            createPickOffsets: e.createPickOffsets
        }
    }, h.unpackCombineGeometryParameters = function (e) {
        for (var t = function (e) {
            for (var t = e, r = new Array(t[0]), n = 0, i = 1; i < t.length;) {
                var o, a = P.Matrix4.unpack(t, i);
                i += P.Matrix4.packedLength, v.defined(t[i]) && (o = {offset: new m(t[i], t[i + 1], t[i + 2])}), i += 3, r[n++] = {
                    modelMatrix: a,
                    attributes: o
                }
            }
            return r
        }(e.packedInstances), r = e.createGeometryResults, n = r.length, i = 0, o = 0; o < n; o++) for (var a = h.unpackCreateGeometryResults(r[o]), s = a.length, d = 0; d < s; d++) {
            var p = a[d];
            t[i].geometry = p, ++i
        }
        var f = u.Ellipsoid.clone(e.ellipsoid);
        return {
            instances: t,
            ellipsoid: f,
            projection: e.isGeographic ? new P.GeographicProjection(f) : new c.WebMercatorProjection(f),
            elementIndexUintSupported: e.elementIndexUintSupported,
            scene3DOnly: e.scene3DOnly,
            vertexCacheOptimize: e.vertexCacheOptimize,
            compressVertices: e.compressVertices,
            modelMatrix: P.Matrix4.clone(e.modelMatrix),
            createPickOffsets: e.createPickOffsets
        }
    }, h.packCombineGeometryResults = function (e, t) {
        v.defined(e.geometries) && function (e, t) {
            for (var r = e.length, n = 0; n < r; ++n) i(e[n], t)
        }(e.geometries, t);
        var r = o(e.boundingSpheres), n = o(e.boundingSpheresCV);
        return t.push(r.buffer, n.buffer), {
            geometries: e.geometries,
            attributeLocations: e.attributeLocations,
            modelMatrix: e.modelMatrix,
            pickOffsets: e.pickOffsets,
            offsetInstanceExtend: e.offsetInstanceExtend,
            boundingSpheres: r,
            boundingSpheresCV: n
        }
    }, h.unpackCombineGeometryResults = function (e) {
        return {
            geometries: e.geometries,
            attributeLocations: e.attributeLocations,
            modelMatrix: e.modelMatrix,
            pickOffsets: e.pickOffsets,
            offsetInstanceExtend: e.offsetInstanceExtend,
            boundingSpheres: r(e.boundingSpheres),
            boundingSpheresCV: r(e.boundingSpheresCV)
        }
    }, e.PrimitivePipeline = h
});
