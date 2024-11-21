#include <core.h>

/**
// REMOVE THIS
#ifdef _WIN32
#if defined(_MSC_VER)
#else
#include <Windows.h>

int APIENTRY WinMain(HINSTANCE hInst, HINSTANCE hInstPrev, PSTR cmdline, int cmdshow)
{
    return sdrpp_main(__argc, __argv);
}
#endif

#endif //_WIN32
**/

int main(int argc, char* argv[]) {
    return sdrpp_main(argc, argv);
}
