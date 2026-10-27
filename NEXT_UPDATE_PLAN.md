# Veylo next-update plan

Updated October 27, 2026. Implementation and publication authorized by the owner on October 27, 2026.

## App analytics

- Enable analytics by default for new installations and existing installations that have never explicitly opted out.
- Preserve explicit opt-outs. Keep a visible switch to disable analytics and request deletion of linked activity.
- Clearly explain the default change when users install or update. Make the analytics setting easier to find, with a short summary and expandable details.
- Keep collection limited to the existing app version, random installation ID, daily activity, and coarse error counts. Exclude viewing history, searches, video identifiers, URLs, and account details.
- Show whether reporting succeeded or failed instead of silently hiding delivery errors.

## Admin dashboard

- Show GitHub APK download totals on Overview, separately from website download clicks and reported active installations.
- Explain each metric and its reporting window; downloads are not unique people or confirmed installations.
- Show collection status, last-received activity, and data freshness. Distinguish unavailable data from a confirmed zero.

## Branding and credits

- Replace the visible "Flow Android Client by A-EDev and contributors" subtitle on the Legal credits row with "Source, licenses, and contributor credits."
- Keep the original-project attribution, copyright notices, and original-source link inside the credits dialog and relevant license files.

## Shorts share button

- Use the approved complete white outlined curved share arrow, directly over the video with no background circle or container.
- Match the other Shorts action icons in visual size, stroke weight, alignment, and label spacing; do not use the enlarged size shown in the mockup.
- Preserve an accessible touch target without enlarging the visible icon, and prevent the arrow from being clipped.

## Verification

- The owner confirmed that enabling analytics on their phone produced a dashboard entry. Keep this genuine opt-in test as the reference for the working collection path.
- Verify new-install defaults, upgrade behavior, preserved opt-outs, opt-out deletion, and genuine activity reaching the dashboard before release.
- Verify that the Legal credits row uses the neutral subtitle and its dialog still displays the original attribution and source link.
- Verify that the share arrow matches adjacent action icons in size and alignment, remains fully visible, and opens the existing share action.
- Investigate website click delivery separately; the earlier live check found zero stored click records despite GitHub-recorded APK downloads.
