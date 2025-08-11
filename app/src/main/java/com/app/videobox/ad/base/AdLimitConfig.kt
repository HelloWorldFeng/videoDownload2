package com.app.videobox.ad.base

data class AdLimitConfig(
    val default_config: SpecialConfig,
    val source: List<String>,
    val special_config: SpecialConfig,
    val user_type: List<String>
)


data class SpecialConfig(
    val int_show_count_one_hours: Int,
    val int_show_interval_time: Int,
    val int_show_count_one_day: Int,
    val int_request_count_one_day: Int,
    val open_show_count_one_hours: Int,
    val open_show_interval_time: Int,
    val open_show_count_one_day: Int,
    val open_request_count_one_day: Int
)