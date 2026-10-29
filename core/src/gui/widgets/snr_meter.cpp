#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <gui/widgets/volume_meter.h>
#include <algorithm>
#include <gui/style.h>

#ifndef IMGUI_DEFINE_MATH_OPERATORS
#define IMGUI_DEFINE_MATH_OPERATORS
#endif
#include <imgui/imgui_internal.h>

namespace ImGui {
    void SNRMeter(float val, const ImVec2& size_arg = ImVec2(0, 0)) {
        ImGuiWindow* window = GetCurrentWindow();
        ImGuiStyle& style = GImGui->Style;

        ImVec2 min = window->DC.CursorPos;
        ImVec2 size = CalcItemSize(size_arg, CalcItemWidth(), roundf(28.0f * style::uiScale));
        ImRect bb(min, min + size);

        ImU32 text = ImGui::GetColorU32(ImGuiCol_Text);

        ItemSize(size, style.FramePadding.y);
        if (!ItemAdd(bb, 0)) {
            return;
        }

        val = std::clamp<float>(val, 0, 100);
        float ratio = size.x / 90;
        float it = size.x / 9;
        char buf[32];

        window->DrawList->AddRectFilled(min + ImVec2(0, 1), min + ImVec2(roundf((float)val * ratio), 10 * style::uiScale), IM_COL32(0, 136, 255, 255));
        window->DrawList->AddLine(min, min + ImVec2(0, (10.0f * style::uiScale) - 1), text, style::uiScale);
        window->DrawList->AddLine(min + ImVec2(0, (10.0f * style::uiScale) - 1), min + ImVec2(size.x + 1, (10.0f * style::uiScale) - 1), text, style::uiScale);

        ImVec2 maxTextSz = ImGui::CalcTextSize("90");
        int step = 1;
        if (it < maxTextSz.x + (6.0f * style::uiScale)) {
            step = 3;
        }
        if ((it * 3.0f) < maxTextSz.x + (4.0f * style::uiScale)) {
            step = 9;
        }

        for (int i = 0; i < 10; i++) {
            bool isMajor = (i % step == 0);
            float tickLen = isMajor ? (15.0f * style::uiScale) : (13.0f * style::uiScale);
            window->DrawList->AddLine(min + ImVec2(roundf((float)i * it), (10.0f * style::uiScale) - 1), min + ImVec2(roundf((float)i * it), tickLen - 1), text, style::uiScale);

            if (isMajor) {
                sprintf(buf, "%d", i * 10);
                ImVec2 sz = ImGui::CalcTextSize(buf);
                float textX = min.x + ((float)i * it) - (sz.x / 2.0f);
                if (i == 0) { textX = std::max<float>(textX, min.x); }
                else if (i == 9) { textX = std::min<float>(textX, min.x + size.x - sz.x); }
                window->DrawList->AddText(ImVec2(roundf(textX), min.y + (16.0f * style::uiScale)), text, buf);
            }
        }
    }
}