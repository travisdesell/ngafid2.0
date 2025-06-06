define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./AttributeCompression-6cb5b251", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./createTaskProcessorWorker", "./EllipsoidTangentPlane-30395e74", "./OrientedBoundingBox-e6450288", "./Color-9e7980a7"], function (Le, e, Oe, Ue, a, r, n, Pe, Fe, t, i, o, s, Se, Re) {
    "use strict";
    var De = new Ue.Cartesian3, Me = new Ue.Ellipsoid, _e = new Ue.Rectangle,
        Ge = {min: void 0, max: void 0, indexBytesPerElement: void 0};

    function Ye(e, a, r) {
        var n = a.length, t = 2 + n * Se.OrientedBoundingBox.packedLength + 1 + function (e) {
            for (var a = e.length, r = 0, n = 0; n < a; ++n) r += Re.Color.packedLength + 3 + e[n].batchIds.length;
            return r
        }(r), i = new Float64Array(t), o = 0;
        i[o++] = e, i[o++] = n;
        for (var s = 0; s < n; ++s) Se.OrientedBoundingBox.pack(a[s], i, o), o += Se.OrientedBoundingBox.packedLength;
        var f = r.length;
        i[o++] = f;
        for (var d = 0; d < f; ++d) {
            var c = r[d];
            Re.Color.pack(c.color, i, o), o += Re.Color.packedLength, i[o++] = c.offset, i[o++] = c.count;
            var u = c.batchIds, h = u.length;
            i[o++] = h;
            for (var l = 0; l < h; ++l) i[o++] = u[l]
        }
        return i
    }

    var Ve = new Ue.Cartesian3, He = new Ue.Cartesian3, We = new Ue.Cartesian3, ze = new Ue.Cartesian3,
        Ze = new Ue.Cartesian3, je = new Ue.Cartographic, qe = new Ue.Rectangle;
    return o(function (e, a) {
        var r, n, t, i;
        r = e.packedBuffer, n = new Float64Array(r), t = 0, Ge.indexBytesPerElement = n[t++], Ge.min = n[t++], Ge.max = n[t++], Ue.Cartesian3.unpack(n, t, De), t += Ue.Cartesian3.packedLength, Ue.Ellipsoid.unpack(n, t, Me), t += Ue.Ellipsoid.packedLength, Ue.Rectangle.unpack(n, t, _e), i = new (2 === Ge.indexBytesPerElement ? Uint16Array : Uint32Array)(e.indices);
        var o, s, f, d = new Uint16Array(e.positions), c = new Uint32Array(e.counts),
            u = new Uint32Array(e.indexCounts), h = new Uint32Array(e.batchIds),
            l = new Uint32Array(e.batchTableColors), g = new Array(c.length), p = De, b = Me, C = _e, y = Ge.min,
            I = Ge.max, m = e.minimumHeights, v = e.maximumHeights;
        Le.defined(m) && Le.defined(v) && (m = new Float32Array(m), v = new Float32Array(v));
        var w = d.length / 2, x = d.subarray(0, w), A = d.subarray(w, 2 * w);
        Pe.AttributeCompression.zigZagDeltaDecode(x, A);
        var E = new Float64Array(3 * w);
        for (o = 0; o < w; ++o) {
            var N = x[o], T = A[o], k = Oe.CesiumMath.lerp(C.west, C.east, N / 32767),
                B = Oe.CesiumMath.lerp(C.south, C.north, T / 32767), L = Ue.Cartographic.fromRadians(k, B, 0, je),
                O = b.cartographicToCartesian(L, Ve);
            Ue.Cartesian3.pack(O, E, 3 * o)
        }
        var U = c.length, P = new Array(U), F = new Array(U), S = 0, R = 0;
        for (o = 0; o < U; ++o) P[o] = S, F[o] = R, S += c[o], R += u[o];
        var D, M = new Float32Array(3 * w * 2), _ = new Uint16Array(2 * w), G = new Uint32Array(F.length),
            Y = new Uint32Array(u.length), V = [], H = {};
        for (o = 0; o < U; ++o) f = l[o], Le.defined(H[f]) ? (H[f].positionLength += c[o], H[f].indexLength += u[o], H[f].batchIds.push(o)) : H[f] = {
            positionLength: c[o],
            indexLength: u[o],
            offset: 0,
            indexOffset: 0,
            batchIds: [o]
        };
        var W = 0, z = 0;
        for (f in H) if (H.hasOwnProperty(f)) {
            (D = H[f]).offset = W, D.indexOffset = z;
            var Z = 2 * D.positionLength, j = 2 * D.indexLength + 6 * D.positionLength;
            W += Z, z += j, D.indexLength = j
        }
        var q = [];
        for (f in H) H.hasOwnProperty(f) && (D = H[f], q.push({
            color: Re.Color.fromRgba(parseInt(f)),
            offset: D.indexOffset,
            count: D.indexLength,
            batchIds: D.batchIds
        }));
        for (o = 0; o < U; ++o) {
            var J = (D = H[f = l[o]]).offset, K = 3 * J, Q = J, X = P[o], $ = c[o], ee = h[o], ae = y, re = I;
            Le.defined(m) && Le.defined(v) && (ae = m[o], re = v[o]);
            var ne = Number.POSITIVE_INFINITY, te = Number.NEGATIVE_INFINITY, ie = Number.POSITIVE_INFINITY,
                oe = Number.NEGATIVE_INFINITY;
            for (s = 0; s < $; ++s) {
                var se = Ue.Cartesian3.unpack(E, 3 * X + 3 * s, Ve);
                b.scaleToGeodeticSurface(se, se);
                var fe = b.cartesianToCartographic(se, je), de = fe.latitude, ce = fe.longitude;
                ne = Math.min(de, ne), te = Math.max(de, te), ie = Math.min(ce, ie), oe = Math.max(ce, oe);
                var ue = b.geodeticSurfaceNormal(se, He), he = Ue.Cartesian3.multiplyByScalar(ue, ae, We),
                    le = Ue.Cartesian3.add(se, he, ze);
                he = Ue.Cartesian3.multiplyByScalar(ue, re, he);
                var ge = Ue.Cartesian3.add(se, he, Ze);
                Ue.Cartesian3.subtract(ge, p, ge), Ue.Cartesian3.subtract(le, p, le), Ue.Cartesian3.pack(ge, M, K), Ue.Cartesian3.pack(le, M, K + 3), _[Q] = ee, _[Q + 1] = ee, K += 6, Q += 2
            }
            (C = qe).west = ie, C.east = oe, C.south = ne, C.north = te, g[o] = Se.OrientedBoundingBox.fromRectangle(C, y, I, b);
            var pe = D.indexOffset, be = F[o], Ce = u[o];
            for (G[o] = pe, s = 0; s < Ce; s += 3) {
                var ye = i[be + s] - X, Ie = i[be + s + 1] - X, me = i[be + s + 2] - X;
                V[pe++] = 2 * ye + J, V[pe++] = 2 * Ie + J, V[pe++] = 2 * me + J, V[pe++] = 2 * me + 1 + J, V[pe++] = 2 * Ie + 1 + J, V[pe++] = 2 * ye + 1 + J
            }
            for (s = 0; s < $; ++s) {
                var ve = s, we = (s + 1) % $;
                V[pe++] = 2 * ve + 1 + J, V[pe++] = 2 * we + J, V[pe++] = 2 * ve + J, V[pe++] = 2 * ve + 1 + J, V[pe++] = 2 * we + 1 + J, V[pe++] = 2 * we + J
            }
            D.offset += 2 * $, D.indexOffset = pe, Y[o] = pe - G[o]
        }
        V = Fe.IndexDatatype.createTypedArray(M.length / 3, V);
        for (var xe = q.length, Ae = 0; Ae < xe; ++Ae) {
            for (var Ee = q[Ae].batchIds, Ne = 0, Te = Ee.length, ke = 0; ke < Te; ++ke) Ne += Y[Ee[ke]];
            q[Ae].count = Ne
        }
        var Be = Ye(2 === V.BYTES_PER_ELEMENT ? Fe.IndexDatatype.UNSIGNED_SHORT : Fe.IndexDatatype.UNSIGNED_INT, g, q);
        return a.push(M.buffer, V.buffer, G.buffer, Y.buffer, _.buffer, Be.buffer), {
            positions: M.buffer,
            indices: V.buffer,
            indexOffsets: G.buffer,
            indexCounts: Y.buffer,
            batchIds: _.buffer,
            packedBuffer: Be.buffer
        }
    })
});
