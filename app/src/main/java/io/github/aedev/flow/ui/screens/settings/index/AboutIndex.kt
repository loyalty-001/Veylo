package io.github.aedev.flow.ui.screens.settings.index

import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.settings.SettingEntry
import io.github.aedev.flow.ui.components.settings.SettingsDestination

internal object AboutIndex {
    private val page = SettingsDestination.ABOUT

    private fun entry(
        key: String,
        title: Int,
        section: Int,
        summary: Int? = null,
    ) = SettingEntry(
        key = "about.$key",
        title = title,
        summary = summary,
        section = section,
        keywords = R.string.settings_keywords_about,
        destination = page,
    )

    val changelog = entry("changelog", R.string.about_changelog, R.string.section_app, R.string.whats_new_in_flow)
    val website = entry("website", R.string.veylo_about_website, R.string.section_contact, R.string.veylo_about_website_address)
    val github = entry("github", R.string.github_label, R.string.section_contact, R.string.github_subtitle)
    val creator = entry("creator", R.string.veylo_original_project, R.string.section_legal, R.string.veylo_original_project_summary)
    val license = entry("license", R.string.about_license, R.string.section_legal, R.string.about_license_name)
    val newPipe = entry("newpipe", R.string.newpipe_extractor_title, R.string.section_legal, R.string.newpipe_extractor_subtitle)
    val notices = entry("notices", R.string.about_notices, R.string.section_legal, R.string.about_notices_summary)
    val deviceInfo = entry("device_info", R.string.about_device_info, R.string.section_device)

    val all = listOf(changelog, website, github, creator, license, newPipe, notices, deviceInfo)
}
