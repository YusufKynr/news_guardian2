# run.py – server’ı başlatmak için
import asyncio, sys, uvicorn

# 1) Doğru loop policy
if sys.platform.startswith("win"):
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

# 2) Uvicorn’u başlat
if __name__ == "__main__":
    uvicorn.run(
        "main:app",   # import yolu
        host="127.0.0.1",
        port=8080,
        reload=True,             # hot-reload yine aktif
        log_level="info",
    )
