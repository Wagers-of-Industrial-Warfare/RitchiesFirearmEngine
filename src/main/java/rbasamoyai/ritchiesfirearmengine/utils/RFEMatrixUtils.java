package rbasamoyai.ritchiesfirearmengine.utils;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import javax.annotation.Nonnull;

/**
 * Taken from Create Big Cannons
 */
public class RFEMatrixUtils {

    /**
     * Constructor for {@link Matrix3f} with the ability to set individual values.
     * 1.20+: simply an alias for the more accessible JOML version.
     */
    public static Matrix3f mat3x3f(float m00, float m01, float m02,
                                   float m10, float m11, float m12,
                                   float m20, float m21, float m22) {
        return new Matrix3f(m00, m01, m02, m10, m11, m12, m20, m21, m22);
    }

    /**
     * Constructor for {@link Matrix4f} with the ability to set individual values.
     * 1.20+: simply an alias for the more accessible JOML version.
     */
    public static Matrix4f mat4x4f(float m00, float m01, float m02, float m03,
                                   float m10, float m11, float m12, float m13,
                                   float m20, float m21, float m22, float m23,
                                   float m30, float m31, float m32, float m33) {
        return new Matrix4f(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33);
    }

    public static Matrix4f mat4x4f(@Nonnull Matrix3f mat3x3f) {
        return mat4x4f(mat3x3f.m00, mat3x3f.m01, mat3x3f.m02, 0,
                       mat3x3f.m10, mat3x3f.m11, mat3x3f.m12, 0,
                       mat3x3f.m20, mat3x3f.m21, mat3x3f.m22, 0,
                       0,           0,           0,           1);
    }

    /**
     * Adapted from Inigo Quilez: <a href="https://iquilezles.org/articles/noacos/">https://iquilezles.org/articles/noacos/</a>
     * <br>This returns the rotation matrix which orients (0, 0, 1) with the input vector.
     *
     * @param normalized Normalized vector pointing in a direction
     * @return {@link Matrix3f} rotating vectors to orient in the specified direction
     */
    public static Matrix3f mat3x3fFacing(Vec3 normalized) {
        // (0, 0, 1) x norm
        float cx = (float) -normalized.y;
        float cy = (float) normalized.x;
        float cos = (float) -normalized.z; // (0, 0, 1) . norm
        float k = 1f / (1f + cos);
        if (cos > -0.9999d)
            return mat3x3f( cx*cx*k+cos, cy*cx*k,      cy,
                            cx*cy*k,     cy*cy*k+cos, -cx,
                           -cy,          cx,           cos);
        return mat3x3f(-1,  0,  0,
                        0,  1,  0,
                        0,  0, -1);
    }

    /**
     * Adapted from Inigo Quilez: <a href="https://iquilezles.org/articles/noacos/">https://iquilezles.org/articles/noacos/</a>
     * <br>This returns the rotation matrix which orients the start vector with the input vector.
     *
     * @param dest Normalized vector pointing in a direction
     * @param source Normalized vector pointing in a direction
     * @return {@link Matrix4f} rotating vectors to orient in the specified direction
     */
    public static Matrix3fc mat3x3fFacing(Vec3 dest, Vec3 source) {
        Vec3 c = source.cross(dest);
        float cx = (float) c.x;
        float cy = (float) c.y;
        float cz = (float) c.z;
        float cos = (float) -source.dot(dest);
        float k = 1f / (1f + cos);
        if (cos > -0.9999d)
            return mat3x3f(cx*cx*k+cos, cy*cx*k-cz,  cz*cx*k+cy,
                           cx*cy*k+cz,  cy*cy*k+cos, cz*cy*k-cx,
                           cx*cz*k-cy,  cy*cz*k+cx,  cz*cz*k+cos);
        if (Math.abs(1 - source.dot(new Vec3(0, 0, 1))) < 1e-4d)
            return mat3x3f(-1,  0,  0,
                            0,  1,  0,
                            0,  0, -1);
        Matrix3f first = mat3x3fFacing(source); // (0, 0, 1) -> source
        first.transpose(first); // source -> (0, 0, 1)
        Matrix3f second = mat3x3fFacing(dest); // (0, 0, 1) -> dest
        second.mul(first, second);
        return second;
    }

    /**
     * {@link #mat3x3fFacing(Vec3)} but padded for {@link Matrix4f}
     * <br>This returns the rotation matrix which orients (0, 0, 1) with the input vector.
     *
     * @param normalized Normalized vector pointing in a direction
     * @return {@link Matrix4f} rotating vectors to orient in the specified direction
     */
    public static Matrix4f mat4x4fFacing(Vec3 normalized) {
        // (0, 0, 1) x norm
        float cx = (float) -normalized.y;
        float cy = (float) normalized.x;
        float cos = (float) -normalized.z; // (0, 0, 1) . norm
        float k = 1f / (1f + cos);
        if (cos > -0.9999d)
            return mat4x4f( cx*cx*k+cos, cy*cx*k,      cy,  0,
                            cx*cy*k,     cy*cy*k+cos, -cx,  0,
                           -cy,          cx,           cos, 0,
                            0,           0,            0,   1);
        return mat4x4f(-1,  0,  0,  0,
                        0,  1,  0,  0,
                        0,  0, -1,  0,
                        0,  0,  0,  1);
    }

    /**
     * {@link #mat3x3fFacing(Vec3, Vec3)} but padded for {@link Matrix4f}
     * <br>This returns the rotation matrix which orients the start vector with the input vector.
     *
     * @param dest Normalized vector pointing in a direction
     * @param source Normalized vector pointing in a direction
     * @return {@link Matrix4f} rotating vectors to orient in the specified direction
     */
    public static Matrix4f mat4x4fFacing(Vec3 dest, Vec3 source) {
        Vec3 c = source.cross(dest);
        float cx = (float) c.x;
        float cy = (float) c.y;
        float cz = (float) c.z;
        float cos = (float) -source.dot(dest);
        float k = 1f / (1f + cos);
        if (cos > -0.9999d)
            return mat4x4f(cx*cx*k+cos, cy*cx*k-cz,  cz*cx*k+cy,  0,
                           cx*cy*k+cz,  cy*cy*k+cos, cz*cy*k-cx,  0,
                           cx*cz*k-cy,  cy*cz*k+cx,  cz*cz*k+cos, 0,
                           0,           0,           0,           1);
        if (Math.abs(1 - source.dot(new Vec3(0, 0, 1))) < 1e-4d)
            return mat4x4f(-1,  0,  0,  0,
                            0,  1,  0,  0,
                            0,  0, -1,  0,
                            0,  0,  0,  1);
        Matrix4fc first = mat4x4fFacing(source); // (0, 0, 1) -> source
        Matrix4f firstD = new Matrix4f();
        first.transpose(firstD); // source -> (0, 0, 1)
        Matrix4fc second = mat4x4fFacing(dest); // (0, 0, 1) -> dest
        Matrix4f result = new Matrix4f();
        second.mul(first, result);
        return result;
    }

}
