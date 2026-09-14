pub const fn const_eval_select<ARG: Tuple, F, G, RET>(
    _arg: ARG,
    _called_in_const: F,
    _called_at_rt: G,
) -> RET
where
    G: FnOnce<ARG, Output = RET>,
    F: const FnOnce<ARG, Output = RET>;
