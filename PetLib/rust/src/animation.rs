use glam::Vec3;

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum AnimationId {
    /// 待机
    Idle,
    /// 行走
    Walking,
    /// 坐下呼吸姿态
    Sitting,
}

/// 单帧动画姿态
/// 所有欧拉角为绝对值，弧度 (x, y, z)，XYZ 序，原始 GLTF 基
#[derive(Clone, Copy)]
pub struct AnimFrame {
    /// 躯干方块欧拉旋转。
    pub body_rot: Vec3,
    /// 头部欧拉旋转。
    pub head_rot: Vec3,
    /// 双臂欧拉旋转 [左, 右]。
    pub arms: [Vec3; 2],
    /// 双腿欧拉旋转 [左, 右]。
    pub legs: [Vec3; 2],
    /// 躯干方块位置增量。
    pub body_pos: Vec3,
    /// 头部位置增量（仅 y）。
    pub head_pos: f32,
    /// 披风欧拉 x。
    pub cape_x: f32,
    /// 披风欧拉 z。
    /// None 表示不触碰骨架当前值。
    pub cape_z: Option<f32>,
}

impl AnimFrame {
    /// resetJoints 等价的静止姿态
    pub fn rest() -> Self {
        Self {
            cape_x: std::f32::consts::PI * 0.06,
            cape_z: Some(0.),
            ..Self::default()
        }
    }

    /// 静止姿态且披风 z 不写
    pub fn rest_with_open_cape() -> Self {
        let mut rest = Self::rest();
        rest.cape_z = None;
        rest
    }
}

impl Default for AnimFrame {
    fn default() -> Self {
        Self {
            body_rot: Vec3::ZERO,
            head_rot: Vec3::ZERO,
            arms: [Vec3::ZERO; 2],
            legs: [Vec3::ZERO; 2],
            body_pos: Vec3::ZERO,
            head_pos: 0.,
            cape_x: 0.,
            cape_z: Some(0.),
        }
    }
}

/// 行走动画的整周期(秒):头/臂/腿/披风各正弦分量周期的最小公倍数。
/// 累计时间按此回绕,避免长时运行后的 f32 相位精度损失。
pub(crate) const WALKING_PERIOD: f32 = 15. * std::f32::consts::PI;

/// t 为累计动画时间（秒，已含速度缩放）。
pub(crate) fn evaluate_walking(t: f32) -> AnimFrame {
    walking(t * 8.)
}

/// 空中松手后的骨骼翻落动作时长（秒）。
pub(crate) const RAGDOLL_DURATION: f32 = 1.35;

/// 空中跌落时的四肢摆动姿态，使用指数阻尼平滑回到基础动画。
pub(crate) fn evaluate_ragdoll(t: f32) -> AnimFrame {
    let time = t.max(0.0);
    let damping = (-3.2 * time).exp();
    let wobble = (time * 15.0).sin() * damping;
    AnimFrame {
        body_rot: Vec3::new(0.10 * damping, 0.0, (0.82 + wobble * 0.16) * damping),
        head_rot: Vec3::new(-0.28 * damping, wobble * 0.10, -0.18 * damping),
        arms: [
            Vec3::new((1.35 + wobble * 0.18) * damping, 0.0, -0.88 * damping),
            Vec3::new((-1.22 - wobble * 0.17) * damping, 0.0, 0.82 * damping),
        ],
        legs: [
            Vec3::new((-0.72 + wobble * 0.20) * damping, 0.0, 0.14 * damping),
            Vec3::new((0.92 - wobble * 0.18) * damping, 0.0, -0.12 * damping),
        ],
        body_pos: Vec3::new(0.0, -0.34 * damping, 0.0),
        head_pos: -0.07 * damping,
        cape_x: std::f32::consts::PI * 0.06 + 0.48 * damping + wobble * 0.08,
        cape_z: Some(wobble * 0.12),
    }
}

/// 坐下姿态的缓慢呼吸周期（秒）。
pub(crate) const SITTING_PERIOD: f32 = std::f32::consts::TAU;

/// 坐下循环：腿向前弯曲、躯干轻微后仰，并保留细小的呼吸/头部动作。
pub(crate) fn evaluate_sitting(t: f32) -> AnimFrame {
    let phase = t.rem_euclid(SITTING_PERIOD);
    let breathe = (phase * 2.0).sin();
    let glance = (phase * 0.7).sin();
    AnimFrame {
        body_rot: Vec3::new(-0.08, 0.0, 0.0),
        head_rot: Vec3::new(glance * 0.025, (phase * 1.1).sin() * 0.035, 0.0),
        arms: [
            Vec3::new(-0.38, 0.0, -0.035),
            Vec3::new(-0.38, 0.0, 0.035),
        ],
        legs: [
            Vec3::new(-1.22 + breathe * 0.012, 0.0, 0.0),
            Vec3::new(-1.22 + breathe * 0.012, 0.0, 0.0),
        ],
        body_pos: Vec3::new(0.0, -0.72 + breathe * 0.018, 0.0),
        head_pos: breathe * 0.015,
        cape_x: std::f32::consts::PI * 0.06,
        cape_z: Some(0.0),
    }
}

/// 行走动画，移植 skinview3d 的 WalkingAnimation
/// https://github.com/bs-community/skinview3d/blob/9b8d22d/src/animation.ts#L153
/// 欧拉为原始 GLTF 基(x/z 相对 skinview3d 渲染基取反)。
fn walking(t: f32) -> AnimFrame {
    const ARM_Z: f32 = std::f32::consts::PI * 0.02;
    AnimFrame {
        body_rot: Vec3::ZERO,
        head_rot: Vec3::new(-(t / 5.).sin() * 0.1, (t / 4.).sin() * 0.2, 0.),
        arms: [
            Vec3::new(
                -(t + std::f32::consts::PI).sin() * 0.5,
                0.,
                -(t.cos() * 0.03 + ARM_Z),
            ),
            Vec3::new(
                -t.sin() * 0.5,
                0.,
                -((t + std::f32::consts::PI).cos() * 0.03 - ARM_Z),
            ),
        ],
        legs: [
            Vec3::new(-t.sin() * 0.5, 0., 0.),
            Vec3::new(-(t + std::f32::consts::PI).sin() * 0.5, 0., 0.),
        ],
        body_pos: Vec3::ZERO,
        head_pos: 0.,
        cape_x: (t / 1.5).sin() * 0.06 + std::f32::consts::PI * 0.06,
        cape_z: None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::pose::{flip_quat, rotation};
    use glam::Quat;

    /// 旧渲染基(换基前)的行走公式,仅测试用:作为共轭映射校验的参照。
    fn walking_reference(t: f32) -> AnimFrame {
        const ARM_Z: f32 = std::f32::consts::PI * 0.02;
        AnimFrame {
            body_rot: Vec3::ZERO,
            head_rot: Vec3::new((t / 5.).sin() * 0.1, (t / 4.).sin() * 0.2, 0.),
            arms: [
                Vec3::new(
                    (t + std::f32::consts::PI).sin() * 0.5,
                    0.,
                    t.cos() * 0.03 + ARM_Z,
                ),
                Vec3::new(
                    t.sin() * 0.5,
                    0.,
                    (t + std::f32::consts::PI).cos() * 0.03 - ARM_Z,
                ),
            ],
            legs: [
                Vec3::new(t.sin() * 0.5, 0., 0.),
                Vec3::new((t + std::f32::consts::PI).sin() * 0.5, 0., 0.),
            ],
            body_pos: Vec3::ZERO,
            head_pos: 0.,
            cape_x: (t / 1.5).sin() * 0.06 + std::f32::consts::PI * 0.06,
            cape_z: None,
        }
    }

    /// 换基校验:原始 GLTF 基的新公式必须与旧渲染基公式呈 180°Y 共轭,
    /// 保证行走观感与换基前逐帧一致。
    #[test]
    fn ragdoll_pose_is_finite_and_damps_toward_rest() {
        let start = evaluate_ragdoll(0.0);
        let middle = evaluate_ragdoll(0.45);
        let end = evaluate_ragdoll(RAGDOLL_DURATION);
        assert!(start.body_rot.is_finite());
        assert!(middle.arms.iter().all(|arm| arm.is_finite()));
        assert!(middle.legs.iter().all(|leg| leg.is_finite()));
        assert!(start.body_rot.z.abs() > end.body_rot.z.abs());
        assert!(start.arms[0].x.abs() > end.arms[0].x.abs());
        assert!(end.body_pos.y.abs() < 0.02);
    }

    #[test]
    fn sitting_pose_is_finite_and_keeps_a_bent_leg_posture() {
        for index in 0..240 {
            let frame = evaluate_sitting(index as f32 / 60.0);
            assert!(frame.body_pos.is_finite());
            assert!(frame.head_rot.is_finite());
            assert!(frame.legs.iter().all(|leg| leg.x < -1.0));
            assert!(frame.arms.iter().all(|arm| arm.is_finite()));
        }
        let first = evaluate_sitting(0.0);
        let wrapped = evaluate_sitting(SITTING_PERIOD);
        assert!(first.body_pos.abs_diff_eq(wrapped.body_pos, 1e-5));
    }

    #[test]
    fn walking_matches_conjugated_reference() {
        let conj = |q: Quat| flip_quat(q).normalize();
        let rot = |e: Vec3| rotation(e.x, e.y, e.z);
        // 同旋判定:归一化四元数点积绝对值 ≈ 1(双覆盖取 abs)。
        // 不用 angle_between:f32 点积舍入到 ≥1 时 acos 会产生 NaN。
        let same = |a: Quat, b: Quat| (a.normalize().dot(b.normalize()).abs() - 1.).abs() < 1e-5;
        for i in 0..240 {
            let t = i as f32 / 30.;
            let new = evaluate_walking(t);
            let old = walking_reference(t * 8.);
            for j in 0..2 {
                assert!(
                    same(rot(new.arms[j]), conj(rot(old.arms[j]))),
                    "arm {j} at {t}"
                );
                assert!(
                    same(rot(new.legs[j]), conj(rot(old.legs[j]))),
                    "leg {j} at {t}"
                );
            }
            assert!(same(rot(new.head_rot), conj(rot(old.head_rot))));
            assert!(same(rot(new.body_rot), conj(rot(old.body_rot))));
            // 披风欧拉的取反已被组合里的 Ry(π) 吸收,换基前后同值
            assert!((new.cape_x - old.cape_x).abs() < 1e-5);
        }
    }

    /// 整周期处与起点姿态一致:骨架可安全地按该周期回绕累计时间
    #[test]
    fn walking_wraps_seamlessly_at_full_period() {
        let a = evaluate_walking(0.);
        let b = evaluate_walking(WALKING_PERIOD);
        for j in 0..2 {
            assert!(a.arms[j].abs_diff_eq(b.arms[j], 1e-3), "arm {j}");
            assert!(a.legs[j].abs_diff_eq(b.legs[j], 1e-3), "leg {j}");
        }
        assert!(a.head_rot.abs_diff_eq(b.head_rot, 1e-3));
        assert!((a.cape_x - b.cape_x).abs() < 1e-3);
    }

    #[test]
    fn walking_is_periodic() {
        let period = std::f32::consts::TAU / 8.;
        let a = evaluate_walking(13.7);
        let b = evaluate_walking(13.7 + period);
        assert!(a.arms[0].abs_diff_eq(b.arms[0], 0.0001));
        assert!(a.legs[1].abs_diff_eq(b.legs[1], 0.0001));
    }

    #[test]
    fn walking_arms_oppose_legs() {
        // t = π/2 时左腿前摆最大,左臂应后摆(反相);原始基下前摆为负 x。
        let f = evaluate_walking(std::f32::consts::FRAC_PI_2 / 8.);
        assert!(f.legs[0].x < -0.4);
        assert!(f.arms[0].x > 0.4);
    }

    #[test]
    fn walking_uses_absolute_z_without_rest_splay() {
        // 行走时手臂 z 分量是绝对值(约 ±0.093 弧度内),不含站姿外张。
        for i in 0..240 {
            let f = evaluate_walking(i as f32 / 60.);
            assert!(f.arms[0].z.abs() < 0.1, "{}", f.arms[0].z);
            assert!(f.arms[1].z.abs() < 0.1);
        }
    }
}
