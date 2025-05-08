interface ScreenshotListener {
    start: (success: () => void, error: (error: any) => void) => void;
}

declare var ScreenshotListener: ScreenshotListener;
