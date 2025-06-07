/* 
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.lowdragmc.lowdraglib2.client.utils.glu;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Project.java
 * <p/>
 * <p/>
 * Created 11-jan-2004
 *
 * @author Erik Duijs
 */
public class Project extends Util {

	private static final float[] IDENTITY_MATRIX =
		new float[] {
			1.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 1.0f, 0.0f,
			0.0f, 0.0f, 0.0f, 1.0f };

	private static final FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
	private static final FloatBuffer finalMatrix = BufferUtils.createFloatBuffer(16);

	private static final FloatBuffer tempMatrix = BufferUtils.createFloatBuffer(16);
	private static final float[] in = new float[4];
	private static final float[] out = new float[4];

	private static final float[] forward = new float[3];
	private static final float[] side = new float[3];
	private static final float[] up = new float[3];

	/**
	 * Make matrix an identity matrix
	 */
	private static void __gluMakeIdentityf(FloatBuffer m) {
		int oldPos = m.position();
		m.put(IDENTITY_MATRIX);
		m.position(oldPos);
	}

	private static void __gluMultMatrixVecf(FloatBuffer m, float[] in, float[] out) {
		for (int i = 0; i < 4; i++) {
			out[i] =
				in[0] * m.get(m.position() + 0*4 + i)
					+ in[1] * m.get(m.position() + 1*4 + i)
					+ in[2] * m.get(m.position() + 2*4 + i)
					+ in[3] * m.get(m.position() + 3*4 + i);

		}
	}

	/**
	 * @param src
	 * @param inverse
	 *
	 * @return true if the matrix was succesfully inverted
	 */
	private static boolean __gluInvertMatrixf(FloatBuffer src, FloatBuffer inverse) {
		int i, j, k, swap;
		float t;
		FloatBuffer temp = Project.tempMatrix;


		for (i = 0; i < 16; i++) {
			temp.put(i, src.get(i + src.position()));
		}
		__gluMakeIdentityf(inverse);

		for (i = 0; i < 4; i++) {
			/*
			 * * Look for largest element in column
			 */
			swap = i;
			for (j = i + 1; j < 4; j++) {
				/*
				 * if (fabs(temp[j][i]) > fabs(temp[i][i])) { swap = j;
				 */
				if (Math.abs(temp.get(j*4 + i)) > Math.abs(temp.get(i* 4 + i))) {
					swap = j;
				}
			}

			if (swap != i) {
				/*
				 * * Swap rows.
				 */
				for (k = 0; k < 4; k++) {
					t = temp.get(i*4 + k);
					temp.put(i*4 + k, temp.get(swap*4 + k));
					temp.put(swap*4 + k, t);

					t = inverse.get(i*4 + k);
					inverse.put(i*4 + k, inverse.get(swap*4 + k));
					//inverse.put((i << 2) + k, inverse.get((swap << 2) + k));
					inverse.put(swap*4 + k, t);
					//inverse.put((swap << 2) + k, t);
				}
			}

			if (temp.get(i*4 + i) == 0) {
				/*
				 * * No non-zero pivot. The matrix is singular, which shouldn't *
				 * happen. This means the user gave us a bad matrix.
				 */
				return false;
			}

			t = temp.get(i*4 + i);
			for (k = 0; k < 4; k++) {
				temp.put(i*4 + k, temp.get(i*4 + k)/t);
				inverse.put(i*4 + k, inverse.get(i*4 + k)/t);
			}
			for (j = 0; j < 4; j++) {
				if (j != i) {
					t = temp.get(j*4 + i);
					for (k = 0; k < 4; k++) {
						temp.put(j*4 + k, temp.get(j*4 + k) - temp.get(i*4 + k) * t);
						inverse.put(j*4 + k, inverse.get(j*4 + k) - inverse.get(i*4 + k) * t);
						/*inverse.put(
							(j << 2) + k,
							inverse.get((j << 2) + k) - inverse.get((i << 2) + k) * t);*/
					}
				}
			}
		}
		return true;
	}

	/**
	 * @param a
	 * @param b
	 * @param r
	 */
	private static void __gluMultMatricesf(FloatBuffer a, FloatBuffer b, FloatBuffer r) {
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				r.put(r.position() + i*4 + j,
					a.get(a.position() + i*4 + 0) * b.get(b.position() + 0*4 + j) + a.get(a.position() + i*4 + 1) * b.get(b.position() + 1*4 + j) + a.get(a.position() + i*4 + 2) * b.get(b.position() + 2*4 + j) + a.get(a.position() + i*4 + 3) * b.get(b.position() + 3*4 + j));
			}
		}
	}

	/**
	 * Method gluProject
	 *
	 * @param objx
	 * @param objy
	 * @param objz
	 * @param modelMatrix
	 * @param projMatrix
	 * @param viewport
	 * @param win_pos
	 */
	public static boolean gluProject(
		float objx,
		float objy,
		float objz,
		FloatBuffer modelMatrix,
		FloatBuffer projMatrix,
		IntBuffer viewport,
		FloatBuffer win_pos) {

		float[] in = Project.in;
		float[] out = Project.out;

		in[0] = objx;
		in[1] = objy;
		in[2] = objz;
		in[3] = 1.0f;

		__gluMultMatrixVecf(modelMatrix, in, out);
		__gluMultMatrixVecf(projMatrix, out, in);

		if (in[3] == 0.0)
			return false;

		in[3] = (1.0f / in[3]) * 0.5f;

		// Map x, y and z to range 0-1
		in[0] = in[0] * in[3] + 0.5f;
		in[1] = in[1] * in[3] + 0.5f;
		in[2] = in[2] * in[3] + 0.5f;

		// Map x,y to viewport
		win_pos.put(0, in[0] * viewport.get(viewport.position() + 2) + viewport.get(viewport.position() + 0));
		win_pos.put(1, in[1] * viewport.get(viewport.position() + 3) + viewport.get(viewport.position() + 1));
		win_pos.put(2, in[2]);

		return true;
	}

	/**
	 * Method gluUnproject
	 *
	 * @param winx
	 * @param winy
	 * @param winz
	 * @param modelMatrix
	 * @param projMatrix
	 * @param viewport
	 * @param obj_pos
	 */
	public static boolean gluUnProject(
		float winx,
		float winy,
		float winz,
		FloatBuffer modelMatrix,
		FloatBuffer projMatrix,
		IntBuffer viewport,
		FloatBuffer obj_pos) {
		float[] in = Project.in;
		float[] out = Project.out;

		__gluMultMatricesf(modelMatrix, projMatrix, finalMatrix);

		if (!__gluInvertMatrixf(finalMatrix, finalMatrix))
			return false;

		in[0] = winx;
		in[1] = winy;
		in[2] = winz;
		in[3] = 1.0f;

		// Map x and y from window coordinates
		in[0] = (in[0] - viewport.get(viewport.position() + 0)) / viewport.get(viewport.position() + 2);
		in[1] = (in[1] - viewport.get(viewport.position() + 1)) / viewport.get(viewport.position() + 3);

		// Map to range -1 to 1
		in[0] = in[0] * 2 - 1;
		in[1] = in[1] * 2 - 1;
		in[2] = in[2] * 2 - 1;

		__gluMultMatrixVecf(finalMatrix, in, out);

		if (out[3] == 0.0)
			return false;

		out[3] = 1.0f / out[3];

		obj_pos.put(obj_pos.position() + 0, out[0] * out[3]);
		obj_pos.put(obj_pos.position() + 1, out[1] * out[3]);
		obj_pos.put(obj_pos.position() + 2, out[2] * out[3]);

		return true;
	}

}
