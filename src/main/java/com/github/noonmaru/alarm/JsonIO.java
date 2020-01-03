/*
 * Copyright (c) 2019 Noonmaru
 *
 * Licensed under the General Public License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.noonmaru.alarm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;


public final class JsonIO
{
	private static Gson gson;
	
	private static JsonParser parser;
	
	private static Gson getGson()
	{
		if (gson == null)
			gson = new GsonBuilder().setPrettyPrinting().create();
		
		return gson;
	}
	
	public static JsonParser getParser()
	{
		if (parser == null)
			parser = new JsonParser();
		
		return parser;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends JsonElement> T load(String s)
	{
		return (T) getParser().parse(s);
	}
	
	public static <T extends JsonElement> T load(File file) throws IOException
	{
		if (!file.exists())
			return null;
		
		BufferedReader reader = null;
		
		try
		{
			reader = new BufferedReader(new FileReader(file));
			
			return load(reader); 
		}
		finally
		{
			if (reader != null)
			{
				reader.close();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends JsonElement> T load(Reader reader)
	{
		return (T) getParser().parse(reader);
	}
	
	public static String save(JsonElement json)
	{
		return getGson().toJson(json);
	}
	
	public static void save(JsonElement json, File file) throws IOException
	{
		File tempFile = new File(file.getPath() + ".tmp");
		BufferedWriter writer = null;
		
		try
		{
			writer = new BufferedWriter(new FileWriter(tempFile));
			save(json, writer);
			writer.close();
			writer = null;

			file.delete();
			tempFile.renameTo(file);
		}
		finally
		{
			if (writer != null)
				try
				{
					writer.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
		}
	}
	
	public static void save(JsonElement json, Appendable writer)
	{
		getGson().toJson(json, writer);
	}
	
	private JsonIO() {}
}
