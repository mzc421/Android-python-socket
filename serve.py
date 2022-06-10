import socket
import base64
from PIL import Image
from io import BytesIO
import detect

while True:
    # 1. 创建 socket 对象
    tcpServe = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    base64_data = ''
    # 2. 将 socket 绑定到指定地址  
    address = ('192.168.123.83', 6666)   # todo  '192.168.0.110', 6666
    tcpServe.bind(address)
    # 3. 接收连接请求
    tcpServe.listen(5)
    print("已开启服务器，等待客户端连接")

    # 4. 等待客户请求一个连接
    tcpClient, addr = tcpServe.accept()
    print('客户端的连接地址', addr)
    # 5. 处理：服务器和客户端通过 send 和 recv 方法通信
    while True:
        data = tcpClient.recv(1024)
        base64_data += str(data, 'utf-8').strip()
        print(base64_data)
        if not data:
            break

    img = base64.b64decode(base64_data)
    image_data = BytesIO(img)
    im = Image.open(image_data)
    im.save("./data/img/pred.jpg")
    # im.show()
    #
    result = detect.predict()
    print(result)
    try:
        message = result[max(result)]
    except:
        message = "0"
    tcpClient.send((message + "\n").encode())  # 二进制  如果字符串不加\n   readline方法将一直阻塞 read方法则不会出现阻塞的情况
    print("预测完成")
    # 6. 传输结束，关闭连接
    tcpClient.close()
    tcpServe.close()


