import asyncio
import re
from websockets.asyncio.server import serve

PORT_JAVA_TEXT = 8080
PORT_JAVA_FILE = 8081
PORT = 8082
HOST = "127.0.0.1"


async def handle_client(ws):
    state: dict = {"client_id": None, "upload_writter": None, "upload_remaining": 0}
    reader, writer = await asyncio.open_connection(HOST, PORT_JAVA_TEXT)

    # receive from browser and send to java
    async def ws_to_tcp():
        try:
            async for msg in ws:
                writer.write((msg + "\n").encode("utf-8"))
                await writer.drain()
        except Exception as e:
            print(f"Websocket -> TCP error: {e}")

    # read from java and send to browser
    async def tcp_to_ws():
        try:
            while True:
                res = await reader.readline()
                # fasly value: False, None, 0, 0.0, "", [], {}, set(), tuple()
                if not res:
                    break
                text: str = res.decode("utf-8").rstrip("\r\n")
                if (
                    text.startswith("[FILE] You accepted the file transfer request")
                    and state["client_id"]
                ):
                    m = re.search(
                        r"for '(.+?)' \((\d+) bytes\)\. \[ID: ([^\]]+)\]", text
                    )
                    if m:
                        file_name: str = m.group(1)
                        file_size: bytes = m.group(2)
                        transfer_id: str = m.group(3)
                        asyncio.create_task(dowload_file(transfer_id, file_name, file_size))
                await ws.send()
        except Exception as e:
            print(f"TCP -> Websocket error: {e}")

    async def dowload_file(transfer_id, file_name, file_size):
        writer = None
        try:
            reader, writer = await asyncio.open_connection(HOST, PORT_JAVA_FILE)
            header = f"[RECEIVER] {transfer_id} {state['client_id']}\n"
            writer.write(header.encode("utf-8"))
            await writer.drain()
            await ws.send(
                f"__WEB_DOWNLOAD_FILE_START__|{transfer_id}|{file_name}|{file_size}"
            )
            remaining = file_size
            while remaining > 0:
                chunk: bytes = await reader.read(min(16384, remaining))
                if not chunk:
                    break
                await ws.send(chunk)
                remaining -= len(chunk)
            await ws.send(f"__WEB_DOWNLOAD_FILE_END__|{transfer_id}")
        except Exception as e:
            print(f"Websocket -> File TCP error: {e}")
        finally:
            if writer is not None:
                writer.close()
                await writer.wait_closed()

    t1: asyncio.Task = asyncio.create_task(ws_to_tcp())
    t2: asyncio.Task = asyncio.create_task(tcp_to_ws())
    try:
        await asyncio.wait([t1, t2], return_when=asyncio.FIRST_COMPLETED)
    finally:
        t1.cancel()
        t2.cancel()
        writer.close()
        await writer.wait_closed()


async def file_transfer():
    header = f"[SENDER]"
    return


async def main():
    server = await serve(handle_client, HOST, PORT)
    print(f"WebSocket Bridge is running at ws://{HOST}:{PORT}")
    await server.serve_forever()


asyncio.run(main())
