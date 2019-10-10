import json
from bs4 import BeautifulSoup
import re
import asyncio
import aiohttp
import time
from pymongo.collection import Collection
from pymongo import MongoClient
import logging
import random

asyncNum = 6

POS = ['上单', '打野', '中单', '下路', '辅助']

header = {
    'Host': 'www.op.gg',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:69.0) Gecko/20100101 Firefox/69.0',
    'Accept': 'application/json, text/javascript, */*; q=0.01',
    'Accept-Language': 'zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2',
    'Accept-Encoding': 'gzip, deflate, br',
    'Connection': 'keep-alive'
}

logging.basicConfig(level=logging.DEBUG,
                    format='%(levelname)s - %(filename)s[%(lineno)d]- %(message)s')

client = MongoClient('localhost', 27017)
db = client.lol
games_collection = Collection(db, 'games')
crawl_history = Collection(db, 'crawl_history')


async def game_spider(spider_name:str, queue: asyncio.Queue):
    while True:
        s = await queue.get()
        name = s['sname']
        sID = s['sid']
        latest_game_time = s['latest_game_time']
        t = latest_game_time
        reach_end = s['reach_end']

        startInfo = str(int(time.time()))
        game_total = 0
        reach_crawled = False
        firstRound = True
        while True:
            async with aiohttp.ClientSession() as session:
                async with session.get(
                        'https://www.op.gg/summoner/matches/ajax/averageAndList/startInfo='
                        + str(startInfo)+'&summonerId=' +
                        str(sID) + '&type=soloranked',
                        headers=header, verify_ssl=False) as game_res:

                    print(spider_name+': crawl for ('+name+')')

                    await asyncio.sleep(random.uniform(0.2,0.8))

                    if game_res.status == 200:
                        ret_list = []
                        game_html = str(json.loads(await game_res.read())['html'])
                        game_html = game_html.replace('\t', '')
                        game_html = game_html.replace('\n', '')
                        soup = BeautifulSoup(game_html, 'lxml')
                        game_list = soup.find_all(
                            'div', class_=re.compile('GameItem (Win|Lose)'))

                        for game in game_list:
                            game_time = int(game['data-game-time'])
                            if game_time <= latest_game_time:
                                if reach_end:
                                    reach_crawled = True
                                    break
                            game_total += 1
                            startInfo = game_time

                            # 这一局游戏的id
                            game_id = game['data-game-id']

                            # 胜负结果
                            game_result = game['data-game-result']

                            # 游戏市场：秒数
                            game_length = str(game.div.div.contents[4].string)[
                                          :-1].split('分 ')
                            game_length = int(
                                game_length[0])*60+int(game_length[1])

                            # 英雄名称
                            champion_name = str(
                                game.div.contents[1].contents[3].a.string).strip()

                            # 补刀数
                            cs = int(
                                str(game.div.contents[3].contents[1].span.string).split()[0])

                            # kda
                            kda = game.div.contents[2].div.contents
                            kill = str(kda[0].string).strip()
                            death = str(kda[2].string).strip()
                            assist = str(kda[4].string).strip()
                            kda = kill+'/'+death+'/'+assist

                            wards = 0
                            # 真眼数目
                            try:
                                wards = int(
                                    game.div.contents[4].contents[1].span.string)
                            except IndexError:
                                pass

                            # 疯狂解析位置
                            teams = game.div.contents[5].contents
                            pos = '未知'
                            for team in teams:
                                players = list(team.contents)
                                for i in range(0, len(players)):
                                    player = players[i]
                                    if len(player['class']) > 1:
                                        pos = POS[i]

                            print(spider_name+': game'+game_id+"@"+str(game_time)+' len=' + str(game_length) + 's ' +
                                  game_result+' 英雄:'+champion_name+'-'+pos+' KDA='+kda+' cs='+str(cs)+' wards='+str(wards))
                            ret_list.append({
                                'gid': game_id,
                                'time': game_time,
                                'length': game_length,
                                'result': game_result,
                                'champion_name': champion_name,
                                'pos': pos,
                                'cs': cs,
                                'kill': int(kill),
                                'death': int(death),
                                'assist': int(assist),
                                'wards': wards
                            })

                        games_collection.update_one(
                            {'sid': sID},
                            {'$addToSet': {'games': {'$each': ret_list}}}
                        )
                        if firstRound and len(ret_list) > 0:
                            t = ret_list[0]['time']
                            firstRound = False
                        if reach_crawled:
                            games_collection.update_one(
                                {'sid': sID},
                                {'$set': {
                                    'latest_game_time': t}}
                            )
                            break

                        print(spider_name+': ('+name+') got ' + str(game_total) + ' games')
                        ret_list = []

                    elif game_res.status == 418:
                        games_collection.update_one(
                            {'sid': sID},
                            {'$set': {'reach_end': True}}
                        )
                        break
                    else:
                        logging.warn(spider_name+': Something went wrong: ' +
                                     str(game_res.status))
                        break

        print("________________________" + spider_name + " got total " +
              str(game_total)+" games for " + name + "________________________\n")
        queue.task_done()

async def main(l:list):
    queue = asyncio.Queue()
    for summoner in l:
        queue.put_nowait(summoner)

    tasks = []
    for i in range(asyncNum):
        task = asyncio.create_task(game_spider(f'SPIDER({i})', queue))
        tasks.append(task)

    await queue.join()

    for task in tasks:
        task.cancel()
    await asyncio.gather(*tasks, return_exceptions=True)

if __name__ == '__main__':
    summoner_last_update_time = crawl_history.find_one(
        {'spider': 'summoner'}
    )['latest']

    # no_games_summoners = games_collection.find(
    #     {'games': {'$exists': True}, 'updateAt': {
    #         '$gte': summoner_last_update_time}},
    #     no_cursor_timeout = True,
    #     batch_size = asyncNum
    # )

    no_games_summoners = games_collection.find(
        {'updateAt': {'$gte': summoner_last_update_time}},
        no_cursor_timeout=True,
        batch_size=asyncNum
    )

    asyncio.run(main(no_games_summoners))
