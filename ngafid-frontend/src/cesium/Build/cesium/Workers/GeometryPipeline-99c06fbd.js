define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./AttributeCompression-6cb5b251", "./EncodedCartesian3-e19aab62", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f"], function (e, _, t, q, U, Y, M, G, P, l, w, Z, a) {
    "use strict";
    var x = new U.Cartesian3, S = new U.Cartesian3, I = new U.Cartesian3;
    var s = {
        calculateACMR: function (e) {
            var t = (e = _.defaultValue(e, _.defaultValue.EMPTY_OBJECT)).indices, r = e.maximumIndex,
                a = _.defaultValue(e.cacheSize, 24), i = t.length;
            if (!_.defined(r)) for (var n = r = 0, s = t[n]; n < i;) r < s && (r = s), s = t[++n];
            for (var o = [], u = 0; u < r + 1; u++) o[u] = 0;
            for (var p = a + 1, d = 0; d < i; ++d) p - o[t[d]] > a && (o[t[d]] = p, ++p);
            return (p - a + 1) / (i / 3)
        }
    };
    s.tipsify = function (e) {
        var v, t = (e = _.defaultValue(e, _.defaultValue.EMPTY_OBJECT)).indices, r = e.maximumIndex,
            a = _.defaultValue(e.cacheSize, 24);

        function i(e, t, r, a, i, n, s) {
            for (var o, u = -1, p = -1, d = 0; d < r.length;) {
                var l = r[d];
                a[l].numLiveTriangles && (o = 0, i - a[l].timeStamp + 2 * a[l].numLiveTriangles <= t && (o = i - a[l].timeStamp), (p < o || -1 === p) && (p = o, u = l)), ++d
            }
            return -1 === u ? function (e, t, r) {
                for (; 1 <= t.length;) {
                    var a = t[t.length - 1];
                    if (t.splice(t.length - 1, 1), 0 < e[a].numLiveTriangles) return a
                }
                for (; v < r;) {
                    if (0 < e[v].numLiveTriangles) return ++v - 1;
                    ++v
                }
                return -1
            }(a, n, s) : u
        }

        var n = t.length, s = 0, o = 0, u = t[o], p = n;
        if (_.defined(r)) s = r + 1; else {
            for (; o < p;) s < u && (s = u), u = t[++o];
            if (-1 === s) return 0;
            ++s
        }
        var d, l = [];
        for (d = 0; d < s; d++) l[d] = {numLiveTriangles: 0, timeStamp: 0, vertexTriangles: []};
        for (var y = o = 0; o < p;) l[t[o]].vertexTriangles.push(y), ++l[t[o]].numLiveTriangles, l[t[o + 1]].vertexTriangles.push(y), ++l[t[o + 1]].numLiveTriangles, l[t[o + 2]].vertexTriangles.push(y), ++l[t[o + 2]].numLiveTriangles, ++y, o += 3;
        var f = 0, c = a + 1;
        v = 1;
        var m, C, h, b, g = [], A = [], T = 0, x = [], P = n / 3, w = [];
        for (d = 0; d < P; d++) w[d] = !1;
        for (; -1 !== f;) {
            g = [], b = (C = l[f]).vertexTriangles.length;
            for (var S = 0; S < b; ++S) if (!w[y = C.vertexTriangles[S]]) {
                w[y] = !0, o = y + y + y;
                for (var I = 0; I < 3; ++I) h = t[o], g.push(h), A.push(h), x[T] = h, ++T, --(m = l[h]).numLiveTriangles, c - m.timeStamp > a && (m.timeStamp = c, ++c), ++o
            }
            f = i(0, a, g, l, c, A, s)
        }
        return x
    };
    var r = {};

    function o(e, t, r, a, i) {
        e[t++] = r, e[t++] = a, e[t++] = a, e[t++] = i, e[t++] = i, e[t] = r
    }

    function f(e) {
        var t = {};
        for (var r in e) if (e.hasOwnProperty(r) && _.defined(e[r]) && _.defined(e[r].values)) {
            var a = e[r];
            t[r] = new G.GeometryAttribute({
                componentDatatype: a.componentDatatype,
                componentsPerAttribute: a.componentsPerAttribute,
                normalize: a.normalize,
                values: []
            })
        }
        return t
    }

    function c(e, t, r) {
        for (var a in t) if (t.hasOwnProperty(a) && _.defined(t[a]) && _.defined(t[a].values)) for (var i = t[a], n = 0; n < i.componentsPerAttribute; ++n) e[a].values.push(i.values[r * i.componentsPerAttribute + n])
    }

    r.toWireframe = function (e) {
        var t = e.indices;
        if (_.defined(t)) {
            switch (e.primitiveType) {
                case G.PrimitiveType.TRIANGLES:
                    e.indices = function (e) {
                        for (var t = e.length, r = t / 3 * 6, a = w.IndexDatatype.createTypedArray(t, r), i = 0, n = 0; n < t; n += 3, i += 6) o(a, i, e[n], e[n + 1], e[n + 2]);
                        return a
                    }(t);
                    break;
                case G.PrimitiveType.TRIANGLE_STRIP:
                    e.indices = function (e) {
                        var t = e.length;
                        if (3 <= t) {
                            var r = 6 * (t - 2), a = w.IndexDatatype.createTypedArray(t, r);
                            o(a, 0, e[0], e[1], e[2]);
                            for (var i = 6, n = 3; n < t; ++n, i += 6) o(a, i, e[n - 1], e[n], e[n - 2]);
                            return a
                        }
                        return new Uint16Array
                    }(t);
                    break;
                case G.PrimitiveType.TRIANGLE_FAN:
                    e.indices = function (e) {
                        if (0 < e.length) {
                            for (var t = e.length - 1, r = 6 * (t - 1), a = w.IndexDatatype.createTypedArray(t, r), i = e[0], n = 0, s = 1; s < t; ++s, n += 6) o(a, n, i, e[s], e[s + 1]);
                            return a
                        }
                        return new Uint16Array
                    }(t)
            }
            e.primitiveType = G.PrimitiveType.LINES
        }
        return e
    }, r.createLineSegmentsForVectors = function (e, t, r) {
        t = _.defaultValue(t, "normal"), r = _.defaultValue(r, 1e4);
        for (var a, i = e.attributes.position.values, n = e.attributes[t].values, s = i.length, o = new Float64Array(2 * s), u = 0, p = 0; p < s; p += 3) o[u++] = i[p], o[u++] = i[p + 1], o[u++] = i[p + 2], o[u++] = i[p] + n[p] * r, o[u++] = i[p + 1] + n[p + 1] * r, o[u++] = i[p + 2] + n[p + 2] * r;
        var d = e.boundingSphere;
        return _.defined(d) && (a = new Y.BoundingSphere(d.center, d.radius + r)), new G.Geometry({
            attributes: {
                position: new G.GeometryAttribute({
                    componentDatatype: M.ComponentDatatype.DOUBLE,
                    componentsPerAttribute: 3,
                    values: o
                })
            }, primitiveType: G.PrimitiveType.LINES, boundingSphere: a
        })
    }, r.createAttributeLocations = function (e) {
        var t,
            r = ["position", "positionHigh", "positionLow", "position3DHigh", "position3DLow", "position2DHigh", "position2DLow", "pickColor", "normal", "st", "tangent", "bitangent", "extrudeDirection", "compressedAttributes"],
            a = e.attributes, i = {}, n = 0, s = r.length;
        for (t = 0; t < s; ++t) {
            var o = r[t];
            _.defined(a[o]) && (i[o] = n++)
        }
        for (var u in a) a.hasOwnProperty(u) && !_.defined(i[u]) && (i[u] = n++);
        return i
    }, r.reorderForPreVertexCache = function (e) {
        var t = G.Geometry.computeNumberOfVertices(e), r = e.indices;
        if (_.defined(r)) {
            for (var a = new Int32Array(t), i = 0; i < t; i++) a[i] = -1;
            for (var n, s = r, o = s.length, u = w.IndexDatatype.createTypedArray(t, o), p = 0, d = 0, l = 0; p < o;) -1 !== (n = a[s[p]]) ? u[d] = n : (a[n = s[p]] = l, u[d] = l, ++l), ++p, ++d;
            e.indices = u;
            var v = e.attributes;
            for (var y in v) if (v.hasOwnProperty(y) && _.defined(v[y]) && _.defined(v[y].values)) {
                for (var f = v[y], c = f.values, m = 0, C = f.componentsPerAttribute, h = M.ComponentDatatype.createTypedArray(f.componentDatatype, l * C); m < t;) {
                    var b = a[m];
                    if (-1 !== b) for (var g = 0; g < C; g++) h[C * b + g] = c[C * m + g];
                    ++m
                }
                f.values = h
            }
        }
        return e
    }, r.reorderForPostVertexCache = function (e, t) {
        var r = e.indices;
        if (e.primitiveType === G.PrimitiveType.TRIANGLES && _.defined(r)) {
            for (var a = r.length, i = 0, n = 0; n < a; n++) r[n] > i && (i = r[n]);
            e.indices = s.tipsify({indices: r, maximumIndex: i, cacheSize: t})
        }
        return e
    }, r.fitToUnsignedShortIndices = function (e) {
        var t = [], r = G.Geometry.computeNumberOfVertices(e);
        if (_.defined(e.indices) && r >= q.CesiumMath.SIXTY_FOUR_KILOBYTES) {
            var a, i = [], n = [], s = 0, o = f(e.attributes), u = e.indices, p = u.length;
            e.primitiveType === G.PrimitiveType.TRIANGLES ? a = 3 : e.primitiveType === G.PrimitiveType.LINES ? a = 2 : e.primitiveType === G.PrimitiveType.POINTS && (a = 1);
            for (var d = 0; d < p; d += a) {
                for (var l = 0; l < a; ++l) {
                    var v = u[d + l], y = i[v];
                    _.defined(y) || (y = s++, i[v] = y, c(o, e.attributes, v)), n.push(y)
                }
                s + a >= q.CesiumMath.SIXTY_FOUR_KILOBYTES && (t.push(new G.Geometry({
                    attributes: o,
                    indices: n,
                    primitiveType: e.primitiveType,
                    boundingSphere: e.boundingSphere,
                    boundingSphereCV: e.boundingSphereCV
                })), i = [], n = [], s = 0, o = f(e.attributes))
            }
            0 !== n.length && t.push(new G.Geometry({
                attributes: o,
                indices: n,
                primitiveType: e.primitiveType,
                boundingSphere: e.boundingSphere,
                boundingSphereCV: e.boundingSphereCV
            }))
        } else t.push(e);
        return t
    };
    var m = new U.Cartesian3, C = new U.Cartographic;
    r.projectTo2D = function (e, t, r, a, i) {
        for (var n = e.attributes[t], s = (i = _.defined(i) ? i : new Y.GeographicProjection).ellipsoid, o = n.values, u = new Float64Array(o.length), p = 0, d = 0; d < o.length; d += 3) {
            var l = U.Cartesian3.fromArray(o, d, m), v = s.cartesianToCartographic(l, C), y = i.project(v, m);
            u[p++] = y.x, u[p++] = y.y, u[p++] = y.z
        }
        return e.attributes[r] = n, e.attributes[a] = new G.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.DOUBLE,
            componentsPerAttribute: 3,
            values: u
        }), delete e.attributes[t], e
    };
    var v = {high: 0, low: 0};
    r.encodeAttribute = function (e, t, r, a) {
        for (var i = e.attributes[t], n = i.values, s = n.length, o = new Float32Array(s), u = new Float32Array(s), p = 0; p < s; ++p) l.EncodedCartesian3.encode(n[p], v), o[p] = v.high, u[p] = v.low;
        var d = i.componentsPerAttribute;
        return e.attributes[r] = new G.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.FLOAT,
            componentsPerAttribute: d,
            values: o
        }), e.attributes[a] = new G.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.FLOAT,
            componentsPerAttribute: d,
            values: u
        }), delete e.attributes[t], e
    };
    var n = new U.Cartesian3;

    function i(e, t) {
        if (_.defined(t)) for (var r = t.values, a = r.length, i = 0; i < a; i += 3) U.Cartesian3.unpack(r, i, n), Y.Matrix4.multiplyByPoint(e, n, n), U.Cartesian3.pack(n, r, i)
    }

    function u(e, t) {
        if (_.defined(t)) for (var r = t.values, a = r.length, i = 0; i < a; i += 3) U.Cartesian3.unpack(r, i, n), Y.Matrix3.multiplyByVector(e, n, n), n = U.Cartesian3.normalize(n, n), U.Cartesian3.pack(n, r, i)
    }

    var p = new Y.Matrix4, d = new Y.Matrix3;
    r.transformToWorldCoordinates = function (e) {
        var t = e.modelMatrix;
        if (Y.Matrix4.equals(t, Y.Matrix4.IDENTITY)) return e;
        var r = e.geometry.attributes;
        i(t, r.position), i(t, r.prevPosition), i(t, r.nextPosition), (_.defined(r.normal) || _.defined(r.tangent) || _.defined(r.bitangent)) && (Y.Matrix4.inverse(t, p), Y.Matrix4.transpose(p, p), Y.Matrix4.getMatrix3(p, d), u(d, r.normal), u(d, r.tangent), u(d, r.bitangent));
        var a = e.geometry.boundingSphere;
        return _.defined(a) && (e.geometry.boundingSphere = Y.BoundingSphere.transform(a, t, a)), e.modelMatrix = Y.Matrix4.clone(Y.Matrix4.IDENTITY), e
    };
    var O = new U.Cartesian3;

    function y(e, t) {
        var r, a, i, n, s, o, u, p, d = e.length, l = (e[0].modelMatrix, _.defined(e[0][t].indices)),
            v = e[0][t].primitiveType, y = function (e, t) {
                var r, a = e.length, i = {}, n = e[0][t].attributes;
                for (r in n) if (n.hasOwnProperty(r) && _.defined(n[r]) && _.defined(n[r].values)) {
                    for (var s = n[r], o = s.values.length, u = !0, p = 1; p < a; ++p) {
                        var d = e[p][t].attributes[r];
                        if (!_.defined(d) || s.componentDatatype !== d.componentDatatype || s.componentsPerAttribute !== d.componentsPerAttribute || s.normalize !== d.normalize) {
                            u = !1;
                            break
                        }
                        o += d.values.length
                    }
                    u && (i[r] = new G.GeometryAttribute({
                        componentDatatype: s.componentDatatype,
                        componentsPerAttribute: s.componentsPerAttribute,
                        normalize: s.normalize,
                        values: M.ComponentDatatype.createTypedArray(s.componentDatatype, o)
                    }))
                }
                return i
            }(e, t);
        for (r in y) if (y.hasOwnProperty(r)) for (s = y[r].values, a = n = 0; a < d; ++a) for (u = (o = e[a][t].attributes[r].values).length, i = 0; i < u; ++i) s[n++] = o[i];
        if (l) {
            var f = 0;
            for (a = 0; a < d; ++a) f += e[a][t].indices.length;
            var c = G.Geometry.computeNumberOfVertices(new G.Geometry({
                attributes: y,
                primitiveType: G.PrimitiveType.POINTS
            })), m = w.IndexDatatype.createTypedArray(c, f), C = 0, h = 0;
            for (a = 0; a < d; ++a) {
                var b = e[a][t].indices, g = b.length;
                for (n = 0; n < g; ++n) m[C++] = h + b[n];
                h += G.Geometry.computeNumberOfVertices(e[a][t])
            }
            p = m
        }
        var A, T = new U.Cartesian3, x = 0;
        for (a = 0; a < d; ++a) {
            if (A = e[a][t].boundingSphere, !_.defined(A)) {
                T = void 0;
                break
            }
            U.Cartesian3.add(A.center, T, T)
        }
        if (_.defined(T)) for (U.Cartesian3.divideByScalar(T, d, T), a = 0; a < d; ++a) {
            A = e[a][t].boundingSphere;
            var P = U.Cartesian3.magnitude(U.Cartesian3.subtract(A.center, T, O)) + A.radius;
            x < P && (x = P)
        }
        return new G.Geometry({
            attributes: y,
            indices: p,
            primitiveType: v,
            boundingSphere: _.defined(T) ? new Y.BoundingSphere(T, x) : void 0
        })
    }

    r.combineInstances = function (e) {
        for (var t = [], r = [], a = e.length, i = 0; i < a; ++i) {
            var n = e[i];
            _.defined(n.geometry) ? t.push(n) : _.defined(n.westHemisphereGeometry) && _.defined(n.eastHemisphereGeometry) && r.push(n)
        }
        var s = [];
        return 0 < t.length && s.push(y(t, "geometry")), 0 < r.length && (s.push(y(r, "westHemisphereGeometry")), s.push(y(r, "eastHemisphereGeometry"))), s
    };
    var T = new U.Cartesian3, E = new U.Cartesian3, N = new U.Cartesian3, L = new U.Cartesian3;
    r.computeNormal = function (e) {
        var t, r = e.indices, a = e.attributes, i = a.position.values, n = a.position.values.length / 3, s = r.length,
            o = new Array(n), u = new Array(s / 3), p = new Array(s);
        for (t = 0; t < n; t++) o[t] = {indexOffset: 0, count: 0, currentCount: 0};
        var d = 0;
        for (t = 0; t < s; t += 3) {
            var l = r[t], v = r[t + 1], y = r[t + 2], f = 3 * l, c = 3 * v, m = 3 * y;
            E.x = i[f], E.y = i[1 + f], E.z = i[2 + f], N.x = i[c], N.y = i[1 + c], N.z = i[2 + c], L.x = i[m], L.y = i[1 + m], L.z = i[2 + m], o[l].count++, o[v].count++, o[y].count++, U.Cartesian3.subtract(N, E, N), U.Cartesian3.subtract(L, E, L), u[d] = U.Cartesian3.cross(N, L, new U.Cartesian3), d++
        }
        var C, h = 0;
        for (t = 0; t < n; t++) o[t].indexOffset += h, h += o[t].count;
        for (t = d = 0; t < s; t += 3) {
            var b = (C = o[r[t]]).indexOffset + C.currentCount;
            p[b] = d, C.currentCount++, p[b = (C = o[r[t + 1]]).indexOffset + C.currentCount] = d, C.currentCount++, p[b = (C = o[r[t + 2]]).indexOffset + C.currentCount] = d, C.currentCount++, d++
        }
        var g = new Float32Array(3 * n);
        for (t = 0; t < n; t++) {
            var A = 3 * t;
            if (C = o[t], U.Cartesian3.clone(U.Cartesian3.ZERO, T), 0 < C.count) {
                for (d = 0; d < C.count; d++) U.Cartesian3.add(T, u[p[C.indexOffset + d]], T);
                U.Cartesian3.equalsEpsilon(U.Cartesian3.ZERO, T, q.CesiumMath.EPSILON10) && U.Cartesian3.clone(u[p[C.indexOffset]], T)
            }
            U.Cartesian3.equalsEpsilon(U.Cartesian3.ZERO, T, q.CesiumMath.EPSILON10) && (T.z = 1), U.Cartesian3.normalize(T, T), g[A] = T.x, g[1 + A] = T.y, g[2 + A] = T.z
        }
        return e.attributes.normal = new G.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: g
        }), e
    };
    var R = new U.Cartesian3, V = new U.Cartesian3, F = new U.Cartesian3;
    r.computeTangentAndBitangent = function (e) {
        e.attributes;
        var t, r, a, i, n = e.indices, s = e.attributes.position.values, o = e.attributes.normal.values,
            u = e.attributes.st.values, p = e.attributes.position.values.length / 3, d = n.length, l = new Array(3 * p);
        for (t = 0; t < l.length; t++) l[t] = 0;
        for (t = 0; t < d; t += 3) {
            var v = n[t], y = n[t + 1], f = n[t + 2];
            a = 3 * y, i = 3 * f;
            var c = 2 * v, m = 2 * y, C = 2 * f, h = s[r = 3 * v], b = s[r + 1], g = s[r + 2], A = u[c], T = u[1 + c],
                x = u[1 + m] - T, P = u[1 + C] - T, w = 1 / ((u[m] - A) * P - (u[C] - A) * x),
                S = (P * (s[a] - h) - x * (s[i] - h)) * w, I = (P * (s[a + 1] - b) - x * (s[i + 1] - b)) * w,
                O = (P * (s[a + 2] - g) - x * (s[i + 2] - g)) * w;
            l[r] += S, l[r + 1] += I, l[r + 2] += O, l[a] += S, l[a + 1] += I, l[a + 2] += O, l[i] += S, l[i + 1] += I, l[i + 2] += O
        }
        var E = new Float32Array(3 * p), N = new Float32Array(3 * p);
        for (t = 0; t < p; t++) {
            a = (r = 3 * t) + 1, i = r + 2;
            var L = U.Cartesian3.fromArray(o, r, R), z = U.Cartesian3.fromArray(l, r, F), D = U.Cartesian3.dot(L, z);
            U.Cartesian3.multiplyByScalar(L, D, V), U.Cartesian3.normalize(U.Cartesian3.subtract(z, V, z), z), E[r] = z.x, E[a] = z.y, E[i] = z.z, U.Cartesian3.normalize(U.Cartesian3.cross(L, z, z), z), N[r] = z.x, N[a] = z.y, N[i] = z.z
        }
        return e.attributes.tangent = new G.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: E
        }), e.attributes.bitangent = new G.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: N
        }), e
    };
    var z = new U.Cartesian2, D = new U.Cartesian3, B = new U.Cartesian3, k = new U.Cartesian3, H = new U.Cartesian2;

    function h(e) {
        switch (e.primitiveType) {
            case G.PrimitiveType.TRIANGLE_FAN:
                return function (e) {
                    var t = G.Geometry.computeNumberOfVertices(e), r = w.IndexDatatype.createTypedArray(t, 3 * (t - 2));
                    r[0] = 1, r[1] = 0, r[2] = 2;
                    for (var a = 3, i = 3; i < t; ++i) r[a++] = i - 1, r[a++] = 0, r[a++] = i;
                    return e.indices = r, e.primitiveType = G.PrimitiveType.TRIANGLES, e
                }(e);
            case G.PrimitiveType.TRIANGLE_STRIP:
                return function (e) {
                    var t = G.Geometry.computeNumberOfVertices(e), r = w.IndexDatatype.createTypedArray(t, 3 * (t - 2));
                    r[0] = 0, r[1] = 1, r[2] = 2, 3 < t && (r[3] = 0, r[4] = 2, r[5] = 3);
                    for (var a = 6, i = 3; i < t - 1; i += 2) r[a++] = i, r[a++] = i - 1, r[a++] = i + 1, i + 2 < t && (r[a++] = i, r[a++] = i + 1, r[a++] = i + 2);
                    return e.indices = r, e.primitiveType = G.PrimitiveType.TRIANGLES, e
                }(e);
            case G.PrimitiveType.TRIANGLES:
                return function (e) {
                    if (_.defined(e.indices)) return e;
                    for (var t = G.Geometry.computeNumberOfVertices(e), r = w.IndexDatatype.createTypedArray(t, t), a = 0; a < t; ++a) r[a] = a;
                    return e.indices = r, e
                }(e);
            case G.PrimitiveType.LINE_STRIP:
                return function (e) {
                    var t = G.Geometry.computeNumberOfVertices(e), r = w.IndexDatatype.createTypedArray(t, 2 * (t - 1));
                    r[0] = 0, r[1] = 1;
                    for (var a = 2, i = 2; i < t; ++i) r[a++] = i - 1, r[a++] = i;
                    return e.indices = r, e.primitiveType = G.PrimitiveType.LINES, e
                }(e);
            case G.PrimitiveType.LINE_LOOP:
                return function (e) {
                    var t = G.Geometry.computeNumberOfVertices(e), r = w.IndexDatatype.createTypedArray(t, 2 * t);
                    r[0] = 0, r[1] = 1;
                    for (var a = 2, i = 2; i < t; ++i) r[a++] = i - 1, r[a++] = i;
                    return r[a++] = t - 1, r[a] = 0, e.indices = r, e.primitiveType = G.PrimitiveType.LINES, e
                }(e);
            case G.PrimitiveType.LINES:
                return function (e) {
                    if (_.defined(e.indices)) return e;
                    for (var t = G.Geometry.computeNumberOfVertices(e), r = w.IndexDatatype.createTypedArray(t, t), a = 0; a < t; ++a) r[a] = a;
                    return e.indices = r, e
                }(e)
        }
        return e
    }

    function b(e, t) {
        Math.abs(e.y) < q.CesiumMath.EPSILON6 && (e.y = t ? -q.CesiumMath.EPSILON6 : q.CesiumMath.EPSILON6)
    }

    r.compressVertices = function (e) {
        var t, r, a = e.attributes.extrudeDirection;
        if (_.defined(a)) {
            var i = a.values;
            r = i.length / 3;
            var n = new Float32Array(2 * r), s = 0;
            for (t = 0; t < r; ++t) U.Cartesian3.fromArray(i, 3 * t, D), U.Cartesian3.equals(D, U.Cartesian3.ZERO) ? s += 2 : (H = P.AttributeCompression.octEncodeInRange(D, 65535, H), n[s++] = H.x, n[s++] = H.y);
            return e.attributes.compressedAttributes = new G.GeometryAttribute({
                componentDatatype: M.ComponentDatatype.FLOAT,
                componentsPerAttribute: 2,
                values: n
            }), delete e.attributes.extrudeDirection, e
        }
        var o = e.attributes.normal, u = e.attributes.st, p = _.defined(o), d = _.defined(u);
        if (!p && !d) return e;
        var l, v, y, f, c = e.attributes.tangent, m = e.attributes.bitangent, C = _.defined(c), h = _.defined(m);
        p && (l = o.values), d && (v = u.values), C && (y = c.values), h && (f = m.values);
        var b = r = (p ? l.length : v.length) / (p ? 3 : 2), g = d && p ? 2 : 1;
        b *= g += C || h ? 1 : 0;
        var A = new Float32Array(b), T = 0;
        for (t = 0; t < r; ++t) {
            d && (U.Cartesian2.fromArray(v, 2 * t, z), A[T++] = P.AttributeCompression.compressTextureCoordinates(z));
            var x = 3 * t;
            p && _.defined(y) && _.defined(f) ? (U.Cartesian3.fromArray(l, x, D), U.Cartesian3.fromArray(y, x, B), U.Cartesian3.fromArray(f, x, k), P.AttributeCompression.octPack(D, B, k, z), A[T++] = z.x, A[T++] = z.y) : (p && (U.Cartesian3.fromArray(l, x, D), A[T++] = P.AttributeCompression.octEncodeFloat(D)), C && (U.Cartesian3.fromArray(y, x, D), A[T++] = P.AttributeCompression.octEncodeFloat(D)), h && (U.Cartesian3.fromArray(f, x, D), A[T++] = P.AttributeCompression.octEncodeFloat(D)))
        }
        return e.attributes.compressedAttributes = new G.GeometryAttribute({
            componentDatatype: M.ComponentDatatype.FLOAT,
            componentsPerAttribute: g,
            values: A
        }), p && delete e.attributes.normal, d && delete e.attributes.st, h && delete e.attributes.bitangent, C && delete e.attributes.tangent, e
    };
    var g = new U.Cartesian3;

    function A(e, t, r, a) {
        U.Cartesian3.add(e, U.Cartesian3.multiplyByScalar(U.Cartesian3.subtract(t, e, g), e.y / (e.y - t.y), g), r), U.Cartesian3.clone(r, a), b(r, !0), b(a, !1)
    }

    var W = new U.Cartesian3, X = new U.Cartesian3, j = new U.Cartesian3, J = new U.Cartesian3,
        K = {positions: new Array(7), indices: new Array(9)};

    function Q(e, t, r) {
        if (!(0 <= e.x || 0 <= t.x || 0 <= r.x)) {
            !function (e, t, r) {
                if (0 !== e.y && 0 !== t.y && 0 !== r.y) return b(e, e.y < 0), b(t, t.y < 0), b(r, r.y < 0);
                var a = Math.abs(e.y), i = Math.abs(t.y), n = Math.abs(r.y),
                    s = (i < a ? n < a ? q.CesiumMath.sign(e.y) : q.CesiumMath.sign(r.y) : n < i ? q.CesiumMath.sign(t.y) : q.CesiumMath.sign(r.y)) < 0;
                b(e, s), b(t, s), b(r, s)
            }(e, t, r);
            var a = e.y < 0, i = t.y < 0, n = r.y < 0, s = 0;
            s += a ? 1 : 0, s += i ? 1 : 0, s += n ? 1 : 0;
            var o = K.indices;
            1 == s ? (o[1] = 3, o[2] = 4, o[5] = 6, o[7] = 6, o[8] = 5, a ? (A(e, t, W, j), A(e, r, X, J), o[0] = 0, o[3] = 1, o[4] = 2, o[6] = 1) : i ? (A(t, r, W, j), A(t, e, X, J), o[0] = 1, o[3] = 2, o[4] = 0, o[6] = 2) : n && (A(r, e, W, j), A(r, t, X, J), o[0] = 2, o[3] = 0, o[4] = 1, o[6] = 0)) : 2 == s && (o[2] = 4, o[4] = 4, o[5] = 3, o[7] = 5, o[8] = 6, a ? i ? n || (A(r, e, W, j), A(r, t, X, J), o[0] = 0, o[1] = 1, o[3] = 0, o[6] = 2) : (A(t, r, W, j), A(t, e, X, J), o[0] = 2, o[1] = 0, o[3] = 2, o[6] = 1) : (A(e, t, W, j), A(e, r, X, J), o[0] = 1, o[1] = 2, o[3] = 1, o[6] = 0));
            var u = K.positions;
            return u[0] = e, u[1] = t, u[2] = r, u.length = 3, 1 != s && 2 != s || (u[3] = W, u[4] = X, u[5] = j, u[6] = J, u.length = 7), K
        }
    }

    function $(e, t) {
        var r = e.attributes;
        if (0 !== r.position.values.length) {
            for (var a in r) if (r.hasOwnProperty(a) && _.defined(r[a]) && _.defined(r[a].values)) {
                var i = r[a];
                i.values = M.ComponentDatatype.createTypedArray(i.componentDatatype, i.values)
            }
            var n = G.Geometry.computeNumberOfVertices(e);
            return e.indices = w.IndexDatatype.createTypedArray(n, e.indices), t && (e.boundingSphere = Y.BoundingSphere.fromVertices(r.position.values)), e
        }
    }

    function ee(e) {
        var t = e.attributes, r = {};
        for (var a in t) if (t.hasOwnProperty(a) && _.defined(t[a]) && _.defined(t[a].values)) {
            var i = t[a];
            r[a] = new G.GeometryAttribute({
                componentDatatype: i.componentDatatype,
                componentsPerAttribute: i.componentsPerAttribute,
                normalize: i.normalize,
                values: []
            })
        }
        return new G.Geometry({attributes: r, indices: [], primitiveType: e.primitiveType})
    }

    function te(e, t, r) {
        var a = _.defined(e.geometry.boundingSphere);
        t = $(t, a), r = $(r, a), _.defined(r) && !_.defined(t) ? e.geometry = r : !_.defined(r) && _.defined(t) ? e.geometry = t : (e.westHemisphereGeometry = t, e.eastHemisphereGeometry = r, e.geometry = void 0)
    }

    function re(v, y) {
        var f = new v, c = new v, m = new v;
        return function (e, t, r, a, i, n, s, o) {
            var u = v.fromArray(i, e * y, f), p = v.fromArray(i, t * y, c), d = v.fromArray(i, r * y, m);
            v.multiplyByScalar(u, a.x, u), v.multiplyByScalar(p, a.y, p), v.multiplyByScalar(d, a.z, d);
            var l = v.add(u, p, u);
            v.add(l, d, l), o && v.normalize(l, l), v.pack(l, n, s * y)
        }
    }

    var ae = re(Y.Cartesian4, 4), ie = re(U.Cartesian3, 3), ne = re(U.Cartesian2, 2),
        se = function (e, t, r, a, i, n, s) {
            var o = i[e] * a.x, u = i[t] * a.y, p = i[r] * a.z;
            n[s] = o + u + p > q.CesiumMath.EPSILON6 ? 1 : 0
        }, oe = new U.Cartesian3, ue = new U.Cartesian3, pe = new U.Cartesian3, de = new U.Cartesian3;

    function le(e, t, r, a, i, n, s, o, u, p, d, l, v, y, f, c) {
        if (_.defined(n) || _.defined(s) || _.defined(o) || _.defined(u) || _.defined(p) || 0 !== y) {
            var m = function (e, t, r, a, i) {
                var n, s, o, u, p, d, l, v;
                if (_.defined(i) || (i = new U.Cartesian3), _.defined(t.z)) {
                    if (U.Cartesian3.equalsEpsilon(e, t, q.CesiumMath.EPSILON14)) return U.Cartesian3.clone(U.Cartesian3.UNIT_X, i);
                    if (U.Cartesian3.equalsEpsilon(e, r, q.CesiumMath.EPSILON14)) return U.Cartesian3.clone(U.Cartesian3.UNIT_Y, i);
                    if (U.Cartesian3.equalsEpsilon(e, a, q.CesiumMath.EPSILON14)) return U.Cartesian3.clone(U.Cartesian3.UNIT_Z, i);
                    n = U.Cartesian3.subtract(r, t, x), s = U.Cartesian3.subtract(a, t, S), o = U.Cartesian3.subtract(e, t, I), u = U.Cartesian3.dot(n, n), p = U.Cartesian3.dot(n, s), d = U.Cartesian3.dot(n, o), l = U.Cartesian3.dot(s, s), v = U.Cartesian3.dot(s, o)
                } else {
                    if (U.Cartesian2.equalsEpsilon(e, t, q.CesiumMath.EPSILON14)) return U.Cartesian3.clone(U.Cartesian3.UNIT_X, i);
                    if (U.Cartesian2.equalsEpsilon(e, r, q.CesiumMath.EPSILON14)) return U.Cartesian3.clone(U.Cartesian3.UNIT_Y, i);
                    if (U.Cartesian2.equalsEpsilon(e, a, q.CesiumMath.EPSILON14)) return U.Cartesian3.clone(U.Cartesian3.UNIT_Z, i);
                    n = U.Cartesian2.subtract(r, t, x), s = U.Cartesian2.subtract(a, t, S), o = U.Cartesian2.subtract(e, t, I), u = U.Cartesian2.dot(n, n), p = U.Cartesian2.dot(n, s), d = U.Cartesian2.dot(n, o), l = U.Cartesian2.dot(s, s), v = U.Cartesian2.dot(s, o)
                }
                i.y = l * d - p * v, i.z = u * v - p * d;
                var y = u * l - p * p;
                return 0 !== i.y && (i.y /= y), 0 !== i.z && (i.z /= y), i.x = 1 - i.y - i.z, i
            }(a, U.Cartesian3.fromArray(i, 3 * e, oe), U.Cartesian3.fromArray(i, 3 * t, ue), U.Cartesian3.fromArray(i, 3 * r, pe), de);
            if (_.defined(n) && ie(e, t, r, m, n, l.normal.values, c, !0), _.defined(p)) {
                var C, h = U.Cartesian3.fromArray(p, 3 * e, oe), b = U.Cartesian3.fromArray(p, 3 * t, ue),
                    g = U.Cartesian3.fromArray(p, 3 * r, pe);
                U.Cartesian3.multiplyByScalar(h, m.x, h), U.Cartesian3.multiplyByScalar(b, m.y, b), U.Cartesian3.multiplyByScalar(g, m.z, g), U.Cartesian3.equals(h, U.Cartesian3.ZERO) && U.Cartesian3.equals(b, U.Cartesian3.ZERO) && U.Cartesian3.equals(g, U.Cartesian3.ZERO) ? ((C = oe).x = 0, C.y = 0, C.z = 0) : (C = U.Cartesian3.add(h, b, h), U.Cartesian3.add(C, g, C), U.Cartesian3.normalize(C, C)), U.Cartesian3.pack(C, l.extrudeDirection.values, 3 * c)
            }
            if (_.defined(d) && se(e, t, r, m, d, l.applyOffset.values, c), _.defined(s) && ie(e, t, r, m, s, l.tangent.values, c, !0), _.defined(o) && ie(e, t, r, m, o, l.bitangent.values, c, !0), _.defined(u) && ne(e, t, r, m, u, l.st.values, c), 0 < y) for (var A = 0; A < y; A++) {
                var T = v[A];
                ve(e, t, r, m, c, f[T], l[T])
            }
        }
    }

    function ve(e, t, r, a, i, n, s) {
        var o = n.componentsPerAttribute, u = n.values, p = s.values;
        switch (o) {
            case 4:
                ae(e, t, r, a, u, p, i, !1);
                break;
            case 3:
                ie(e, t, r, a, u, p, i, !1);
                break;
            case 2:
                ne(e, t, r, a, u, p, i, !1);
                break;
            default:
                p[i] = u[e] * a.x + u[t] * a.y + u[r] * a.z
        }
    }

    function ye(e, t, r, a, i, n) {
        var s = e.position.values.length / 3;
        if (-1 === i) return e.position.values.push(n.x, n.y, n.z), t.push(s), s;
        var o = a[i], u = r[o];
        return -1 === u ? (r[o] = s, e.position.values.push(n.x, n.y, n.z), t.push(s), s) : (t.push(u), u)
    }

    var fe = {position: !0, normal: !0, bitangent: !0, tangent: !0, st: !0, extrudeDirection: !0, applyOffset: !0};

    function ce(e) {
        var t = e.geometry, r = t.attributes, a = r.position.values, i = _.defined(r.normal) ? r.normal.values : void 0,
            n = _.defined(r.bitangent) ? r.bitangent.values : void 0,
            s = _.defined(r.tangent) ? r.tangent.values : void 0, o = _.defined(r.st) ? r.st.values : void 0,
            u = _.defined(r.extrudeDirection) ? r.extrudeDirection.values : void 0,
            p = _.defined(r.applyOffset) ? r.applyOffset.values : void 0, d = t.indices, l = [];
        for (var v in r) r.hasOwnProperty(v) && !fe[v] && _.defined(r[v]) && l.push(v);
        var y, f, c, m, C = l.length, h = ee(t), b = ee(t), g = [];
        g.length = a.length / 3;
        var A = [];
        for (A.length = a.length / 3, m = 0; m < g.length; ++m) g[m] = -1, A[m] = -1;
        var T = d.length;
        for (m = 0; m < T; m += 3) {
            var x = d[m], P = d[m + 1], w = d[m + 2], S = U.Cartesian3.fromArray(a, 3 * x),
                I = U.Cartesian3.fromArray(a, 3 * P), O = U.Cartesian3.fromArray(a, 3 * w), E = Q(S, I, O);
            if (_.defined(E) && 3 < E.positions.length) for (var N = E.positions, L = E.indices, z = L.length, D = 0; D < z; ++D) {
                var M = L[D], G = N[M];
                c = G.y < 0 ? (y = b.attributes, f = b.indices, g) : (y = h.attributes, f = h.indices, A), le(x, P, w, G, a, i, s, n, o, u, p, y, l, C, r, ye(y, f, c, d, M < 3 ? m + M : -1, G))
            } else _.defined(E) && (S = E.positions[0], I = E.positions[1], O = E.positions[2]), c = S.y < 0 ? (y = b.attributes, f = b.indices, g) : (y = h.attributes, f = h.indices, A), le(x, P, w, S, a, i, s, n, o, u, p, y, l, C, r, ye(y, f, c, d, m, S)), le(x, P, w, I, a, i, s, n, o, u, p, y, l, C, r, ye(y, f, c, d, m + 1, I)), le(x, P, w, O, a, i, s, n, o, u, p, y, l, C, r, ye(y, f, c, d, m + 2, O))
        }
        te(e, b, h)
    }

    var me = a.Plane.fromPointNormal(U.Cartesian3.ZERO, U.Cartesian3.UNIT_Y), Ce = new U.Cartesian3,
        he = new U.Cartesian3;

    function be(e, t, r, a, i, n, s) {
        if (_.defined(s)) {
            var o = U.Cartesian3.fromArray(a, 3 * e, oe);
            U.Cartesian3.equalsEpsilon(o, r, q.CesiumMath.EPSILON10) ? n.applyOffset.values[i] = s[e] : n.applyOffset.values[i] = s[t]
        }
    }

    function ge(e) {
        var t, r = e.geometry, a = r.attributes, i = a.position.values,
            n = _.defined(a.applyOffset) ? a.applyOffset.values : void 0, s = r.indices, o = ee(r), u = ee(r),
            p = s.length, d = [];
        d.length = i.length / 3;
        var l = [];
        for (l.length = i.length / 3, t = 0; t < d.length; ++t) d[t] = -1, l[t] = -1;
        for (t = 0; t < p; t += 2) {
            var v = s[t], y = s[t + 1], f = U.Cartesian3.fromArray(i, 3 * v, oe),
                c = U.Cartesian3.fromArray(i, 3 * y, ue);
            Math.abs(f.y) < q.CesiumMath.EPSILON6 && (f.y < 0 ? f.y = -q.CesiumMath.EPSILON6 : f.y = q.CesiumMath.EPSILON6), Math.abs(c.y) < q.CesiumMath.EPSILON6 && (c.y < 0 ? c.y = -q.CesiumMath.EPSILON6 : c.y = q.CesiumMath.EPSILON6);
            var m = o.attributes, C = o.indices, h = l, b = u.attributes, g = u.indices, A = d,
                T = Z.IntersectionTests.lineSegmentPlane(f, c, me, pe);
            if (_.defined(T)) {
                var x = U.Cartesian3.multiplyByScalar(U.Cartesian3.UNIT_Y, 5 * q.CesiumMath.EPSILON9, Ce);
                f.y < 0 && (U.Cartesian3.negate(x, x), m = u.attributes, C = u.indices, h = d, b = o.attributes, g = o.indices, A = l);
                var P = U.Cartesian3.add(T, x, he);
                be(v, y, f, i, ye(m, C, h, s, t, f), m, n), be(v, y, P, i, ye(m, C, h, s, -1, P), m, n), U.Cartesian3.negate(x, x), U.Cartesian3.add(T, x, P), be(v, y, P, i, ye(b, g, A, s, -1, P), b, n), be(v, y, c, i, ye(b, g, A, s, t + 1, c), b, n)
            } else {
                var w, S, I;
                I = f.y < 0 ? (w = u.attributes, S = u.indices, d) : (w = o.attributes, S = o.indices, l), be(v, y, f, i, ye(w, S, I, s, t, f), w, n), be(v, y, c, i, ye(w, S, I, s, t + 1, c), w, n)
            }
        }
        te(e, u, o)
    }

    var Ae = new U.Cartesian2, Te = new U.Cartesian2, xe = new U.Cartesian3, Pe = new U.Cartesian3,
        we = new U.Cartesian3, Se = new U.Cartesian3, Ie = new U.Cartesian3, Oe = new U.Cartesian3,
        Ee = new Y.Cartesian4;

    function Ne(e) {
        for (var t = e.attributes, r = t.position.values, a = t.prevPosition.values, i = t.nextPosition.values, n = r.length, s = 0; s < n; s += 3) {
            var o = U.Cartesian3.unpack(r, s, xe);
            if (!(0 < o.x)) {
                var u = U.Cartesian3.unpack(a, s, Pe);
                (o.y < 0 && 0 < u.y || 0 < o.y && u.y < 0) && (0 < s - 3 ? (a[s] = r[s - 3], a[s + 1] = r[s - 2], a[s + 2] = r[s - 1]) : U.Cartesian3.pack(o, a, s));
                var p = U.Cartesian3.unpack(i, s, we);
                (o.y < 0 && 0 < p.y || 0 < o.y && p.y < 0) && (s + 3 < n ? (i[s] = r[s + 3], i[s + 1] = r[s + 4], i[s + 2] = r[s + 5]) : U.Cartesian3.pack(o, i, s))
            }
        }
    }

    var Le = 5 * q.CesiumMath.EPSILON9, ze = q.CesiumMath.EPSILON6;
    r.splitLongitude = function (e) {
        var t = e.geometry, r = t.boundingSphere;
        if (_.defined(r) && (0 < r.center.x - r.radius || Y.BoundingSphere.intersectPlane(r, a.Plane.ORIGIN_ZX_PLANE) !== Y.Intersect.INTERSECTING)) return e;
        if (t.geometryType !== G.GeometryType.NONE) switch (t.geometryType) {
            case G.GeometryType.POLYLINES:
                !function (e) {
                    var t, r, a, i = e.geometry, n = i.attributes, s = n.position.values, o = n.prevPosition.values,
                        u = n.nextPosition.values, p = n.expandAndWidth.values,
                        d = _.defined(n.st) ? n.st.values : void 0, l = _.defined(n.color) ? n.color.values : void 0,
                        v = ee(i), y = ee(i), f = !1, c = s.length / 3;
                    for (t = 0; t < c; t += 4) {
                        var m = t, C = t + 2, h = U.Cartesian3.fromArray(s, 3 * m, xe),
                            b = U.Cartesian3.fromArray(s, 3 * C, Pe);
                        if (Math.abs(h.y) < ze) for (h.y = ze * (b.y < 0 ? -1 : 1), s[3 * t + 1] = h.y, s[3 * (t + 1) + 1] = h.y, r = 3 * m; r < 3 * m + 12; r += 3) o[r] = s[3 * t], o[r + 1] = s[3 * t + 1], o[r + 2] = s[3 * t + 2];
                        if (Math.abs(b.y) < ze) for (b.y = ze * (h.y < 0 ? -1 : 1), s[3 * (t + 2) + 1] = b.y, s[3 * (t + 3) + 1] = b.y, r = 3 * m; r < 3 * m + 12; r += 3) u[r] = s[3 * (t + 2)], u[r + 1] = s[3 * (t + 2) + 1], u[r + 2] = s[3 * (t + 2) + 2];
                        var g = v.attributes, A = v.indices, T = y.attributes, x = y.indices,
                            P = Z.IntersectionTests.lineSegmentPlane(h, b, me, Se);
                        if (_.defined(P)) {
                            f = !0;
                            var w = U.Cartesian3.multiplyByScalar(U.Cartesian3.UNIT_Y, Le, Ie);
                            h.y < 0 && (U.Cartesian3.negate(w, w), g = y.attributes, A = y.indices, T = v.attributes, x = v.indices);
                            var S = U.Cartesian3.add(P, w, Oe);
                            g.position.values.push(h.x, h.y, h.z, h.x, h.y, h.z), g.position.values.push(S.x, S.y, S.z), g.position.values.push(S.x, S.y, S.z), g.prevPosition.values.push(o[3 * m], o[3 * m + 1], o[3 * m + 2]), g.prevPosition.values.push(o[3 * m + 3], o[3 * m + 4], o[3 * m + 5]), g.prevPosition.values.push(h.x, h.y, h.z, h.x, h.y, h.z), g.nextPosition.values.push(S.x, S.y, S.z), g.nextPosition.values.push(S.x, S.y, S.z), g.nextPosition.values.push(S.x, S.y, S.z), g.nextPosition.values.push(S.x, S.y, S.z), U.Cartesian3.negate(w, w), U.Cartesian3.add(P, w, S), T.position.values.push(S.x, S.y, S.z), T.position.values.push(S.x, S.y, S.z), T.position.values.push(b.x, b.y, b.z, b.x, b.y, b.z), T.prevPosition.values.push(S.x, S.y, S.z), T.prevPosition.values.push(S.x, S.y, S.z), T.prevPosition.values.push(S.x, S.y, S.z), T.prevPosition.values.push(S.x, S.y, S.z), T.nextPosition.values.push(b.x, b.y, b.z, b.x, b.y, b.z), T.nextPosition.values.push(u[3 * C], u[3 * C + 1], u[3 * C + 2]), T.nextPosition.values.push(u[3 * C + 3], u[3 * C + 4], u[3 * C + 5]);
                            var I = U.Cartesian2.fromArray(p, 2 * m, Ae), O = Math.abs(I.y);
                            g.expandAndWidth.values.push(-1, O, 1, O), g.expandAndWidth.values.push(-1, -O, 1, -O), T.expandAndWidth.values.push(-1, O, 1, O), T.expandAndWidth.values.push(-1, -O, 1, -O);
                            var E = U.Cartesian3.magnitudeSquared(U.Cartesian3.subtract(P, h, we));
                            if (E /= U.Cartesian3.magnitudeSquared(U.Cartesian3.subtract(b, h, we)), _.defined(l)) {
                                var N = Y.Cartesian4.fromArray(l, 4 * m, Ee), L = Y.Cartesian4.fromArray(l, 4 * C, Ee),
                                    z = q.CesiumMath.lerp(N.x, L.x, E), D = q.CesiumMath.lerp(N.y, L.y, E),
                                    M = q.CesiumMath.lerp(N.z, L.z, E), G = q.CesiumMath.lerp(N.w, L.w, E);
                                for (r = 4 * m; r < 4 * m + 8; ++r) g.color.values.push(l[r]);
                                for (g.color.values.push(z, D, M, G), g.color.values.push(z, D, M, G), T.color.values.push(z, D, M, G), T.color.values.push(z, D, M, G), r = 4 * C; r < 4 * C + 8; ++r) T.color.values.push(l[r])
                            }
                            if (_.defined(d)) {
                                var R = U.Cartesian2.fromArray(d, 2 * m, Ae),
                                    V = U.Cartesian2.fromArray(d, 2 * (t + 3), Te), F = q.CesiumMath.lerp(R.x, V.x, E);
                                for (r = 2 * m; r < 2 * m + 4; ++r) g.st.values.push(d[r]);
                                for (g.st.values.push(F, R.y), g.st.values.push(F, V.y), T.st.values.push(F, R.y), T.st.values.push(F, V.y), r = 2 * C; r < 2 * C + 4; ++r) T.st.values.push(d[r])
                            }
                            a = g.position.values.length / 3 - 4, A.push(a, a + 2, a + 1), A.push(a + 1, a + 2, a + 3), a = T.position.values.length / 3 - 4, x.push(a, a + 2, a + 1), x.push(a + 1, a + 2, a + 3)
                        } else {
                            var B, k;
                            for (k = h.y < 0 ? (B = y.attributes, y.indices) : (B = v.attributes, v.indices), B.position.values.push(h.x, h.y, h.z), B.position.values.push(h.x, h.y, h.z), B.position.values.push(b.x, b.y, b.z), B.position.values.push(b.x, b.y, b.z), r = 3 * t; r < 3 * t + 12; ++r) B.prevPosition.values.push(o[r]), B.nextPosition.values.push(u[r]);
                            for (r = 2 * t; r < 2 * t + 8; ++r) B.expandAndWidth.values.push(p[r]), _.defined(d) && B.st.values.push(d[r]);
                            if (_.defined(l)) for (r = 4 * t; r < 4 * t + 16; ++r) B.color.values.push(l[r]);
                            a = B.position.values.length / 3 - 4, k.push(a, a + 2, a + 1), k.push(a + 1, a + 2, a + 3)
                        }
                    }
                    f && (Ne(y), Ne(v)), te(e, y, v)
                }(e);
                break;
            case G.GeometryType.TRIANGLES:
                ce(e);
                break;
            case G.GeometryType.LINES:
                ge(e)
        } else h(t), t.primitiveType === G.PrimitiveType.TRIANGLES ? ce(e) : t.primitiveType === G.PrimitiveType.LINES && ge(e);
        return e
    }, e.GeometryPipeline = r
});
