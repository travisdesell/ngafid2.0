define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./GeometryPipeline-99c06fbd", "./IndexDatatype-e3260434", "./GeometryOffsetAttribute-e6e9672c", "./VertexFormat-ad523db1", "./EllipseGeometryLibrary-de353ee5", "./GeometryInstance-b43ca1c5"], function (e, U, t, y, Q, W, J, q, Z, b, v, K, f, B, C) {
    "use strict";
    var X = new Q.Cartesian3, $ = new Q.Cartesian3, ee = new Q.Cartesian3, te = new Q.Cartesian3, re = new Q.Cartesian2,
        ae = new W.Matrix3, Y = new W.Matrix3, ie = new W.Quaternion, ne = new Q.Cartesian3, oe = new Q.Cartesian3,
        se = new Q.Cartesian3, le = new Q.Cartographic, ue = new Q.Cartesian3, me = new Q.Cartesian2,
        pe = new Q.Cartesian2;

    function w(e, t, r) {
        var a = t.vertexFormat, i = t.center, n = t.semiMajorAxis, o = t.semiMinorAxis, s = t.ellipsoid,
            l = t.stRotation, u = r ? e.length / 3 * 2 : e.length / 3, m = t.shadowVolume,
            p = a.st ? new Float32Array(2 * u) : void 0, y = a.normal ? new Float32Array(3 * u) : void 0,
            c = a.tangent ? new Float32Array(3 * u) : void 0, d = a.bitangent ? new Float32Array(3 * u) : void 0,
            f = m ? new Float32Array(3 * u) : void 0, A = 0, h = ne, x = oe, g = se, _ = new W.GeographicProjection(s),
            b = _.project(s.cartesianToCartographic(i, le), ue), v = s.scaleToGeodeticSurface(i, X);
        s.geodeticSurfaceNormal(v, v);
        var C = ae, w = Y;
        if (0 !== l) {
            var M = W.Quaternion.fromAxisAngle(v, l, ie);
            C = W.Matrix3.fromQuaternion(M, C), M = W.Quaternion.fromAxisAngle(v, -l, ie), w = W.Matrix3.fromQuaternion(M, w)
        } else C = W.Matrix3.clone(W.Matrix3.IDENTITY, C), w = W.Matrix3.clone(W.Matrix3.IDENTITY, w);
        for (var E = Q.Cartesian2.fromElements(Number.POSITIVE_INFINITY, Number.POSITIVE_INFINITY, me), I = Q.Cartesian2.fromElements(Number.NEGATIVE_INFINITY, Number.NEGATIVE_INFINITY, pe), T = e.length, G = r ? T : 0, N = G / 3 * 2, P = 0; P < T; P += 3) {
            var F = P + 1, V = P + 2, D = Q.Cartesian3.fromArray(e, P, X);
            if (a.st) {
                var O = W.Matrix3.multiplyByVector(C, D, $), S = _.project(s.cartesianToCartographic(O, le), ee);
                Q.Cartesian3.subtract(S, b, S), re.x = (S.x + n) / (2 * n), re.y = (S.y + o) / (2 * o), E.x = Math.min(re.x, E.x), E.y = Math.min(re.y, E.y), I.x = Math.max(re.x, I.x), I.y = Math.max(re.y, I.y), r && (p[A + N] = re.x, p[A + 1 + N] = re.y), p[A++] = re.x, p[A++] = re.y
            }
            (a.normal || a.tangent || a.bitangent || m) && (h = s.geodeticSurfaceNormal(D, h), m && (f[P + G] = -h.x, f[F + G] = -h.y, f[V + G] = -h.z), (a.normal || a.tangent || a.bitangent) && ((a.tangent || a.bitangent) && (x = Q.Cartesian3.normalize(Q.Cartesian3.cross(Q.Cartesian3.UNIT_Z, h, x), x), W.Matrix3.multiplyByVector(w, x, x)), a.normal && (y[P] = h.x, y[F] = h.y, y[V] = h.z, r && (y[P + G] = -h.x, y[F + G] = -h.y, y[V + G] = -h.z)), a.tangent && (c[P] = x.x, c[F] = x.y, c[V] = x.z, r && (c[P + G] = -x.x, c[F + G] = -x.y, c[V + G] = -x.z)), a.bitangent && (g = Q.Cartesian3.normalize(Q.Cartesian3.cross(h, x, g), g), d[P] = g.x, d[F] = g.y, d[V] = g.z, r && (d[P + G] = g.x, d[F + G] = g.y, d[V + G] = g.z))))
        }
        if (a.st) {
            T = p.length;
            for (var L = 0; L < T; L += 2) p[L] = (p[L] - E.x) / (I.x - E.x), p[L + 1] = (p[L + 1] - E.y) / (I.y - E.y)
        }
        var R = new Z.GeometryAttributes;
        if (a.position) {
            var j = B.EllipseGeometryLibrary.raisePositionsToHeight(e, t, r);
            R.position = new q.GeometryAttribute({
                componentDatatype: J.ComponentDatatype.DOUBLE,
                componentsPerAttribute: 3,
                values: j
            })
        }
        if (a.st && (R.st = new q.GeometryAttribute({
            componentDatatype: J.ComponentDatatype.FLOAT,
            componentsPerAttribute: 2,
            values: p
        })), a.normal && (R.normal = new q.GeometryAttribute({
            componentDatatype: J.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: y
        })), a.tangent && (R.tangent = new q.GeometryAttribute({
            componentDatatype: J.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: c
        })), a.bitangent && (R.bitangent = new q.GeometryAttribute({
            componentDatatype: J.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: d
        })), m && (R.extrudeDirection = new q.GeometryAttribute({
            componentDatatype: J.ComponentDatatype.FLOAT,
            componentsPerAttribute: 3,
            values: f
        })), r && U.defined(t.offsetAttribute)) {
            var k = new Uint8Array(u);
            if (t.offsetAttribute === K.GeometryOffsetAttribute.TOP) k = K.arrayFill(k, 1, 0, u / 2); else {
                var z = t.offsetAttribute === K.GeometryOffsetAttribute.NONE ? 0 : 1;
                k = K.arrayFill(k, z)
            }
            R.applyOffset = new q.GeometryAttribute({
                componentDatatype: J.ComponentDatatype.UNSIGNED_BYTE,
                componentsPerAttribute: 1,
                values: k
            })
        }
        return R
    }

    function M(e) {
        var t, r, a, i, n, o = new Array(e * (e + 1) * 12 - 6), s = 0;
        for (a = 1, i = t = 0; i < 3; i++) o[s++] = a++, o[s++] = t, o[s++] = a;
        for (i = 2; i < e + 1; ++i) {
            for (a = i * (i + 1) - 1, t = (i - 1) * i - 1, o[s++] = a++, o[s++] = t, o[s++] = a, r = 2 * i, n = 0; n < r - 1; ++n) o[s++] = a, o[s++] = t++, o[s++] = t, o[s++] = a++, o[s++] = t, o[s++] = a;
            o[s++] = a++, o[s++] = t, o[s++] = a
        }
        for (r = 2 * e, ++a, ++t, i = 0; i < r - 1; ++i) o[s++] = a, o[s++] = t++, o[s++] = t, o[s++] = a++, o[s++] = t, o[s++] = a;
        for (o[s++] = a, o[s++] = t++, o[s++] = t, o[s++] = a++, o[s++] = t++, o[s++] = t, ++t, i = e - 1; 1 < i; --i) {
            for (o[s++] = t++, o[s++] = t, o[s++] = a, r = 2 * i, n = 0; n < r - 1; ++n) o[s++] = a, o[s++] = t++, o[s++] = t, o[s++] = a++, o[s++] = t, o[s++] = a;
            o[s++] = t++, o[s++] = t++, o[s++] = a++
        }
        for (i = 0; i < 3; i++) o[s++] = t++, o[s++] = t, o[s++] = a;
        return o
    }

    var u = new Q.Cartesian3;
    var E = new W.BoundingSphere, I = new W.BoundingSphere;

    function m(e) {
        var t = e.center, r = e.ellipsoid, a = e.semiMajorAxis,
            i = Q.Cartesian3.multiplyByScalar(r.geodeticSurfaceNormal(t, X), e.height, X);
        E.center = Q.Cartesian3.add(t, i, E.center), E.radius = a, i = Q.Cartesian3.multiplyByScalar(r.geodeticSurfaceNormal(t, i), e.extrudedHeight, i), I.center = Q.Cartesian3.add(t, i, I.center), I.radius = a;
        var n = B.EllipseGeometryLibrary.computeEllipsePositions(e, !0, !0), o = n.positions, s = n.numPts,
            l = n.outerPositions, u = W.BoundingSphere.union(E, I), m = w(o, e, !0), p = M(s), y = p.length;
        p.length = 2 * y;
        for (var c = o.length / 3, d = 0; d < y; d += 3) p[d + y] = p[d + 2] + c, p[d + 1 + y] = p[d + 1] + c, p[d + 2 + y] = p[d] + c;
        var f = v.IndexDatatype.createTypedArray(2 * c / 3, p),
            A = new q.Geometry({attributes: m, indices: f, primitiveType: q.PrimitiveType.TRIANGLES}),
            h = function (e, t) {
                var r = t.vertexFormat, a = t.center, i = t.semiMajorAxis, n = t.semiMinorAxis, o = t.ellipsoid,
                    s = t.height, l = t.extrudedHeight, u = t.stRotation, m = e.length / 3 * 2,
                    p = new Float64Array(3 * m), y = r.st ? new Float32Array(2 * m) : void 0,
                    c = r.normal ? new Float32Array(3 * m) : void 0, d = r.tangent ? new Float32Array(3 * m) : void 0,
                    f = r.bitangent ? new Float32Array(3 * m) : void 0, A = t.shadowVolume,
                    h = A ? new Float32Array(3 * m) : void 0, x = 0, g = ne, _ = oe, b = se,
                    v = new W.GeographicProjection(o), C = v.project(o.cartesianToCartographic(a, le), ue),
                    w = o.scaleToGeodeticSurface(a, X);
                o.geodeticSurfaceNormal(w, w);
                for (var M = W.Quaternion.fromAxisAngle(w, u, ie), E = W.Matrix3.fromQuaternion(M, ae), I = Q.Cartesian2.fromElements(Number.POSITIVE_INFINITY, Number.POSITIVE_INFINITY, me), T = Q.Cartesian2.fromElements(Number.NEGATIVE_INFINITY, Number.NEGATIVE_INFINITY, pe), G = e.length, N = G / 3 * 2, P = 0; P < G; P += 3) {
                    var F, V = P + 1, D = P + 2, O = Q.Cartesian3.fromArray(e, P, X);
                    if (r.st) {
                        var S = W.Matrix3.multiplyByVector(E, O, $),
                            L = v.project(o.cartesianToCartographic(S, le), ee);
                        Q.Cartesian3.subtract(L, C, L), re.x = (L.x + i) / (2 * i), re.y = (L.y + n) / (2 * n), I.x = Math.min(re.x, I.x), I.y = Math.min(re.y, I.y), T.x = Math.max(re.x, T.x), T.y = Math.max(re.y, T.y), y[x + N] = re.x, y[x + 1 + N] = re.y, y[x++] = re.x, y[x++] = re.y
                    }
                    O = o.scaleToGeodeticSurface(O, O), F = Q.Cartesian3.clone(O, $), g = o.geodeticSurfaceNormal(O, g), A && (h[P + G] = -g.x, h[V + G] = -g.y, h[D + G] = -g.z);
                    var R = Q.Cartesian3.multiplyByScalar(g, s, te);
                    if (O = Q.Cartesian3.add(O, R, O), R = Q.Cartesian3.multiplyByScalar(g, l, R), F = Q.Cartesian3.add(F, R, F), r.position && (p[P + G] = F.x, p[V + G] = F.y, p[D + G] = F.z, p[P] = O.x, p[V] = O.y, p[D] = O.z), r.normal || r.tangent || r.bitangent) {
                        b = Q.Cartesian3.clone(g, b);
                        var j = Q.Cartesian3.fromArray(e, (P + 3) % G, te);
                        Q.Cartesian3.subtract(j, O, j);
                        var k = Q.Cartesian3.subtract(F, O, ee);
                        g = Q.Cartesian3.normalize(Q.Cartesian3.cross(k, j, g), g), r.normal && (c[P] = g.x, c[V] = g.y, c[D] = g.z, c[P + G] = g.x, c[V + G] = g.y, c[D + G] = g.z), r.tangent && (_ = Q.Cartesian3.normalize(Q.Cartesian3.cross(b, g, _), _), d[P] = _.x, d[V] = _.y, d[D] = _.z, d[P + G] = _.x, d[P + 1 + G] = _.y, d[P + 2 + G] = _.z), r.bitangent && (f[P] = b.x, f[V] = b.y, f[D] = b.z, f[P + G] = b.x, f[V + G] = b.y, f[D + G] = b.z)
                    }
                }
                if (r.st) {
                    G = y.length;
                    for (var z = 0; z < G; z += 2) y[z] = (y[z] - I.x) / (T.x - I.x), y[z + 1] = (y[z + 1] - I.y) / (T.y - I.y)
                }
                var B = new Z.GeometryAttributes;
                if (r.position && (B.position = new q.GeometryAttribute({
                    componentDatatype: J.ComponentDatatype.DOUBLE,
                    componentsPerAttribute: 3,
                    values: p
                })), r.st && (B.st = new q.GeometryAttribute({
                    componentDatatype: J.ComponentDatatype.FLOAT,
                    componentsPerAttribute: 2,
                    values: y
                })), r.normal && (B.normal = new q.GeometryAttribute({
                    componentDatatype: J.ComponentDatatype.FLOAT,
                    componentsPerAttribute: 3,
                    values: c
                })), r.tangent && (B.tangent = new q.GeometryAttribute({
                    componentDatatype: J.ComponentDatatype.FLOAT,
                    componentsPerAttribute: 3,
                    values: d
                })), r.bitangent && (B.bitangent = new q.GeometryAttribute({
                    componentDatatype: J.ComponentDatatype.FLOAT,
                    componentsPerAttribute: 3,
                    values: f
                })), A && (B.extrudeDirection = new q.GeometryAttribute({
                    componentDatatype: J.ComponentDatatype.FLOAT,
                    componentsPerAttribute: 3,
                    values: h
                })), U.defined(t.offsetAttribute)) {
                    var Y = new Uint8Array(m);
                    if (t.offsetAttribute === K.GeometryOffsetAttribute.TOP) Y = K.arrayFill(Y, 1, 0, m / 2); else {
                        var H = t.offsetAttribute === K.GeometryOffsetAttribute.NONE ? 0 : 1;
                        Y = K.arrayFill(Y, H)
                    }
                    B.applyOffset = new q.GeometryAttribute({
                        componentDatatype: J.ComponentDatatype.UNSIGNED_BYTE,
                        componentsPerAttribute: 1,
                        values: Y
                    })
                }
                return B
            }(l, e);
        p = function (e) {
            for (var t = e.length / 3, r = v.IndexDatatype.createTypedArray(t, 6 * t), a = 0, i = 0; i < t; i++) {
                var n = i + t, o = (i + 1) % t, s = o + t;
                r[a++] = i, r[a++] = n, r[a++] = o, r[a++] = o, r[a++] = n, r[a++] = s
            }
            return r
        }(l);
        var x = v.IndexDatatype.createTypedArray(2 * l.length / 3, p),
            g = new q.Geometry({attributes: h, indices: x, primitiveType: q.PrimitiveType.TRIANGLES}),
            _ = b.GeometryPipeline.combineInstances([new C.GeometryInstance({geometry: A}), new C.GeometryInstance({geometry: g})]);
        return {boundingSphere: u, attributes: _[0].attributes, indices: _[0].indices}
    }

    function s(e, t, r, a, i, n, o) {
        for (var s = B.EllipseGeometryLibrary.computeEllipsePositions({
            center: e,
            semiMajorAxis: t,
            semiMinorAxis: r,
            rotation: a,
            granularity: i
        }, !1, !0).outerPositions, l = s.length / 3, u = new Array(l), m = 0; m < l; ++m) u[m] = Q.Cartesian3.fromArray(s, 3 * m);
        var p = Q.Rectangle.fromCartesianArray(u, n, o);
        return p.width > y.CesiumMath.PI && (p.north = 0 < p.north ? y.CesiumMath.PI_OVER_TWO - y.CesiumMath.EPSILON7 : p.north, p.south = p.south < 0 ? y.CesiumMath.EPSILON7 - y.CesiumMath.PI_OVER_TWO : p.south, p.east = y.CesiumMath.PI, p.west = -y.CesiumMath.PI), p
    }

    function A(e) {
        var t = (e = U.defaultValue(e, U.defaultValue.EMPTY_OBJECT)).center,
            r = U.defaultValue(e.ellipsoid, Q.Ellipsoid.WGS84), a = e.semiMajorAxis, i = e.semiMinorAxis,
            n = U.defaultValue(e.granularity, y.CesiumMath.RADIANS_PER_DEGREE),
            o = U.defaultValue(e.vertexFormat, f.VertexFormat.DEFAULT), s = U.defaultValue(e.height, 0),
            l = U.defaultValue(e.extrudedHeight, s);
        this._center = Q.Cartesian3.clone(t), this._semiMajorAxis = a, this._semiMinorAxis = i, this._ellipsoid = Q.Ellipsoid.clone(r), this._rotation = U.defaultValue(e.rotation, 0), this._stRotation = U.defaultValue(e.stRotation, 0), this._height = Math.max(l, s), this._granularity = n, this._vertexFormat = f.VertexFormat.clone(o), this._extrudedHeight = Math.min(l, s), this._shadowVolume = U.defaultValue(e.shadowVolume, !1), this._workerName = "createEllipseGeometry", this._offsetAttribute = e.offsetAttribute, this._rectangle = void 0, this._textureCoordinateRotationPoints = void 0
    }

    A.packedLength = Q.Cartesian3.packedLength + Q.Ellipsoid.packedLength + f.VertexFormat.packedLength + 9, A.pack = function (e, t, r) {
        return r = U.defaultValue(r, 0), Q.Cartesian3.pack(e._center, t, r), r += Q.Cartesian3.packedLength, Q.Ellipsoid.pack(e._ellipsoid, t, r), r += Q.Ellipsoid.packedLength, f.VertexFormat.pack(e._vertexFormat, t, r), r += f.VertexFormat.packedLength, t[r++] = e._semiMajorAxis, t[r++] = e._semiMinorAxis, t[r++] = e._rotation, t[r++] = e._stRotation, t[r++] = e._height, t[r++] = e._granularity, t[r++] = e._extrudedHeight, t[r++] = e._shadowVolume ? 1 : 0, t[r] = U.defaultValue(e._offsetAttribute, -1), t
    };
    var h = new Q.Cartesian3, x = new Q.Ellipsoid, g = new f.VertexFormat, _ = {
        center: h,
        ellipsoid: x,
        vertexFormat: g,
        semiMajorAxis: void 0,
        semiMinorAxis: void 0,
        rotation: void 0,
        stRotation: void 0,
        height: void 0,
        granularity: void 0,
        extrudedHeight: void 0,
        shadowVolume: void 0,
        offsetAttribute: void 0
    };
    A.unpack = function (e, t, r) {
        t = U.defaultValue(t, 0);
        var a = Q.Cartesian3.unpack(e, t, h);
        t += Q.Cartesian3.packedLength;
        var i = Q.Ellipsoid.unpack(e, t, x);
        t += Q.Ellipsoid.packedLength;
        var n = f.VertexFormat.unpack(e, t, g);
        t += f.VertexFormat.packedLength;
        var o = e[t++], s = e[t++], l = e[t++], u = e[t++], m = e[t++], p = e[t++], y = e[t++], c = 1 === e[t++],
            d = e[t];
        return U.defined(r) ? (r._center = Q.Cartesian3.clone(a, r._center), r._ellipsoid = Q.Ellipsoid.clone(i, r._ellipsoid), r._vertexFormat = f.VertexFormat.clone(n, r._vertexFormat), r._semiMajorAxis = o, r._semiMinorAxis = s, r._rotation = l, r._stRotation = u, r._height = m, r._granularity = p, r._extrudedHeight = y, r._shadowVolume = c, r._offsetAttribute = -1 === d ? void 0 : d, r) : (_.height = m, _.extrudedHeight = y, _.granularity = p, _.stRotation = u, _.rotation = l, _.semiMajorAxis = o, _.semiMinorAxis = s, _.shadowVolume = c, _.offsetAttribute = -1 === d ? void 0 : d, new A(_))
    }, A.computeRectangle = function (e, t) {
        var r = (e = U.defaultValue(e, U.defaultValue.EMPTY_OBJECT)).center,
            a = U.defaultValue(e.ellipsoid, Q.Ellipsoid.WGS84), i = e.semiMajorAxis, n = e.semiMinorAxis,
            o = U.defaultValue(e.granularity, y.CesiumMath.RADIANS_PER_DEGREE);
        return s(r, i, n, U.defaultValue(e.rotation, 0), o, a, t)
    }, A.createGeometry = function (e) {
        if (!(e._semiMajorAxis <= 0 || e._semiMinorAxis <= 0)) {
            var t = e._height, r = e._extrudedHeight, a = !y.CesiumMath.equalsEpsilon(t, r, 0, y.CesiumMath.EPSILON2);
            e._center = e._ellipsoid.scaleToGeodeticSurface(e._center, e._center);
            var i, n = {
                center: e._center,
                semiMajorAxis: e._semiMajorAxis,
                semiMinorAxis: e._semiMinorAxis,
                ellipsoid: e._ellipsoid,
                rotation: e._rotation,
                height: t,
                granularity: e._granularity,
                vertexFormat: e._vertexFormat,
                stRotation: e._stRotation
            };
            if (a) n.extrudedHeight = r, n.shadowVolume = e._shadowVolume, n.offsetAttribute = e._offsetAttribute, i = m(n); else if (i = function (e) {
                var t = e.center;
                u = Q.Cartesian3.multiplyByScalar(e.ellipsoid.geodeticSurfaceNormal(t, u), e.height, u), u = Q.Cartesian3.add(t, u, u);
                var r = new W.BoundingSphere(u, e.semiMajorAxis),
                    a = B.EllipseGeometryLibrary.computeEllipsePositions(e, !0, !1), i = a.positions, n = a.numPts,
                    o = w(i, e, !1), s = M(n);
                return {
                    boundingSphere: r,
                    attributes: o,
                    indices: s = v.IndexDatatype.createTypedArray(i.length / 3, s)
                }
            }(n), U.defined(e._offsetAttribute)) {
                var o = i.attributes.position.values.length, s = new Uint8Array(o / 3),
                    l = e._offsetAttribute === K.GeometryOffsetAttribute.NONE ? 0 : 1;
                K.arrayFill(s, l), i.attributes.applyOffset = new q.GeometryAttribute({
                    componentDatatype: J.ComponentDatatype.UNSIGNED_BYTE,
                    componentsPerAttribute: 1,
                    values: s
                })
            }
            return new q.Geometry({
                attributes: i.attributes,
                indices: i.indices,
                primitiveType: q.PrimitiveType.TRIANGLES,
                boundingSphere: i.boundingSphere,
                offsetAttribute: e._offsetAttribute
            })
        }
    }, A.createShadowVolume = function (e, t, r) {
        var a = e._granularity, i = e._ellipsoid, n = t(a, i), o = r(a, i);
        return new A({
            center: e._center,
            semiMajorAxis: e._semiMajorAxis,
            semiMinorAxis: e._semiMinorAxis,
            ellipsoid: i,
            rotation: e._rotation,
            stRotation: e._stRotation,
            granularity: a,
            extrudedHeight: n,
            height: o,
            vertexFormat: f.VertexFormat.POSITION_ONLY,
            shadowVolume: !0
        })
    }, Object.defineProperties(A.prototype, {
        rectangle: {
            get: function () {
                return U.defined(this._rectangle) || (this._rectangle = s(this._center, this._semiMajorAxis, this._semiMinorAxis, this._rotation, this._granularity, this._ellipsoid)), this._rectangle
            }
        }, textureCoordinateRotationPoints: {
            get: function () {
                return U.defined(this._textureCoordinateRotationPoints) || (this._textureCoordinateRotationPoints = function (e) {
                    var t = -e._stRotation;
                    if (0 == t) return [0, 0, 0, 1, 1, 0];
                    for (var r = B.EllipseGeometryLibrary.computeEllipsePositions({
                        center: e._center,
                        semiMajorAxis: e._semiMajorAxis,
                        semiMinorAxis: e._semiMinorAxis,
                        rotation: e._rotation,
                        granularity: e._granularity
                    }, !1, !0).outerPositions, a = r.length / 3, i = new Array(a), n = 0; n < a; ++n) i[n] = Q.Cartesian3.fromArray(r, 3 * n);
                    var o = e._ellipsoid, s = e.rectangle;
                    return q.Geometry._textureCoordinateRotationPoints(i, t, o, s)
                }(this)), this._textureCoordinateRotationPoints
            }
        }
    }), e.EllipseGeometry = A
});
